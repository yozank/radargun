package org.radargun.config;

import org.radargun.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Radim Vansa <rvansa@redhat.com>
 * @version 11/21/12
 */
public class ConfigSchemaGenerator {
   public static final String NS_XS = "http://www.w3.org/2001/XMLSchema";
   public static final String RG_PREFIX = "rg:";
   public static final String XS_ELEMENT = "element";
   public static final String XS_COMPLEX_TYPE = "complexType";
   public static final String XS_SEQUENCE = "sequence";
   public static final String XS_MIN_OCCURS = "minOccurs";
   public static final String XS_MAX_OCCURS = "maxOccurs";
   public static final String XS_ATTRIBUTE = "attribute";
   public static final String XS_NAME = "name";
   public static final String XS_TYPE = "type";
   public static final String XS_COMPLEX_CONTENT = "complexContent";
   public static final String XS_EXTENSION = "extension";
   public static final String XS_BASE = "base";
   public static final String XS_ABSTRACT = "abstract";
   public static final String RG_ABSTRACT_PRODUCT = RG_PREFIX + "abstractProduct";
   public static final String XS_CHOICE = "choice";
   private static List<String> products = new ArrayList<String>();
   private static String stageJarFile;

   public static void generate(String directory) {
      try {
         PrintWriter writer = new PrintWriter(new File(directory + File.separator + "radargun-1.1.xsd"));
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document doc = builder.newDocument();
         doc.setXmlVersion("1.0");
         doc.setXmlStandalone(true);
         generate(doc);

         TransformerFactory tf = TransformerFactory.newInstance();
         tf.setAttribute("indent-number", 3);
         Transformer trans = tf.newTransformer();
         trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
         trans.setOutputProperty(OutputKeys.INDENT, "yes");


         StreamResult result = new StreamResult(writer);
         DOMSource source = new DOMSource(doc);
         trans.transform(source, result);
         writer.flush();
      } catch (FileNotFoundException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (ParserConfigurationException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (TransformerConfigurationException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (TransformerException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   private static void generate(Document doc) {
      Element schema = doc.createElementNS(NS_XS, "schema");
      schema.setAttribute("attributeFormDefault", "unqualified");
      schema.setAttribute("elementFormDefault", "qualified");
      schema.setAttribute("version", "1.0");
      schema.setAttribute("targetNamespace", "urn:radargun:benchmark:1.1");
      schema.setAttribute("xmlns:rg", "urn:radargun:benchmark:1.1");
      doc.appendChild(schema);

      Element benchConfig = doc.createElementNS(NS_XS, XS_ELEMENT);
      benchConfig.setAttribute(XS_NAME, "bench-config");
      schema.appendChild(benchConfig);
      Element benchConfigComplex = doc.createElementNS(NS_XS, XS_COMPLEX_TYPE);
      benchConfig.appendChild(benchConfigComplex);
      Element benchConfigSequence = createSequence(doc, benchConfigComplex);

      Element masterComplex = createComplexElement(doc, benchConfigSequence, "master", 1, 1);
      addAttribute(doc, masterComplex, "bindAddress");
      addAttribute(doc, masterComplex, "port");

      Element benchmarkComplex = createComplexElement(doc, benchConfigSequence, "benchmark", 1, 1);
      Element benchmarkChoice = createChoice(doc, createSequence(doc, benchmarkComplex), 1, -1);
      Element repeatType = createComplexType(doc, schema, "repeat", null, false);
      Element repeatChoice = createChoice(doc, createSequence(doc, repeatType), 1, -1);
      addAttribute(doc, repeatType, "times");
      addAttribute(doc, repeatType, "from");
      addAttribute(doc, repeatType, "to");
      addAttribute(doc, repeatType, "inc");
      addAttribute(doc, repeatType, "name");
      createReference(doc, benchmarkChoice, "Repeat", RG_PREFIX + "repeat");
      createReference(doc, repeatChoice, "Repeat", RG_PREFIX + "repeat");
      generateStageDefinitions(doc, schema, new Element[]{benchmarkChoice, repeatChoice});

      addAttribute(doc, benchmarkComplex, "initSize");
      addAttribute(doc, benchmarkComplex, "maxSize");
      addAttribute(doc, benchmarkComplex, "increment");

      Element productsComplex = createComplexElement(doc, benchConfigSequence, "products", 1, 1);
      Element productsSequence = createSequence(doc, productsComplex);

      Element abstractProductType = createComplexType(doc, schema, "abstractProduct", null, true);
      Element abstractProductSequence = createSequence(doc, abstractProductType);
      Element configComplex = createComplexElement(doc, abstractProductSequence, "config", 1, -1);
      Element configSequence = createSequence(doc, configComplex);
      addAttribute(doc, configComplex, "name");
      addAttribute(doc, configComplex, "file");
      addAttribute(doc, configComplex, "cache");
      Element siteComplex = createComplexElement(doc, configSequence, "site", 0, -1);
      addAttribute(doc, siteComplex, "name");
      addAttribute(doc, siteComplex, "config");
      addAttribute(doc, siteComplex, "slaves");
      addAttribute(doc, siteComplex, "cache");

      Element productChoice = createChoice(doc, productsSequence, 1, -1);
      Element genericProductType = createComplexType(doc, schema, "product", RG_ABSTRACT_PRODUCT, false);
      addAttribute(doc, genericProductType, "name");
      createReference(doc, productChoice, "product", RG_PREFIX + "product");
      for (String productName : products) {
         createComplexType(doc, schema, productName, RG_ABSTRACT_PRODUCT, false);
         createReference(doc, productChoice, productName, RG_PREFIX + productName);
      }

      Element reportsComplex = createComplexElement(doc, benchConfigSequence, "reports", 1, 1);
      Element reportsSequence = createSequence(doc, reportsComplex);
      Element reportComplex = createComplexElement(doc, reportsSequence, "report", 0, -1);
      Element reportSequence = createSequence(doc, reportComplex);
      addAttribute(doc, reportComplex, "name");
      addAttribute(doc, reportComplex, "includeAll");
      Element itemComplex = createComplexElement(doc, reportSequence, "item", 0, -1);
      addAttribute(doc, itemComplex, "product");
      addAttribute(doc, itemComplex, "config");
   }

   private static void generateStageDefinitions(Document doc, Element schema, Element[] parents) {
      Set<Class> generatedStages = new HashSet<Class>();
      for (String stageName : getStageNames()) {
         Class stage;
         try {
            stage = Class.forName("org.radargun.stages." + stageName + "Stage");
            if (generatedStages.contains(stage)) continue;
         } catch (ClassNotFoundException e) {
            System.err.println("Cannot load class for stage " + stageName);
            continue;
         }
         generateStage(doc, schema, parents, stage, generatedStages);
      }
   }

   private static void generateStage(Document doc, Element schema, Element[] parents, Class stage, Set<Class> generatedStages) {
      if (generatedStages.contains(stage)) return;
      boolean hasParentStage = Stage.class.isAssignableFrom(stage.getSuperclass());
      if (hasParentStage) {
         generateStage(doc, schema, parents, stage.getSuperclass(), generatedStages);
      }
      Element stageType = createComplexType(doc, schema, getStageName(stage),
            hasParentStage ? RG_PREFIX + getStageName(stage.getSuperclass()) : null, Modifier.isAbstract(stage.getModifiers()));
      for (Element parent : parents) {
         String name = getStageName(stage);
         createReference(doc, parent, name, RG_PREFIX + name);
      }
      Pattern pattern = Pattern.compile("set(\\p{Upper}.*)");
      for (Method m : stage.getMethods()) {
         Matcher matcher;
         if (m.getDeclaringClass().equals(stage) && Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) &&
               (matcher = pattern.matcher(m.getName())).matches()) {
            try {
               stage.getSuperclass().getMethod(m.getName(), m.getParameterTypes());
               continue;
            } catch (NoSuchMethodException e) { }
            String property = matcher.group(1).substring(0, 1).toLowerCase(Locale.ENGLISH) + matcher.group(1).substring(1);
            addAttribute(doc, stageType, property);
         }
      }
      generatedStages.add(stage);
   }

   private static String getStageName(Class stage) {
      String name = stage.getSimpleName();
      if (!name.endsWith("Stage")) throw new IllegalArgumentException(stage.getCanonicalName());
      return name.substring(0, name.length() - 5);
   }

   private static List<String> getStageNames() {
      List<String> stageNames = new ArrayList<String>();
      try {
         ZipInputStream inputStream = new ZipInputStream(new FileInputStream(stageJarFile));
         Pattern pattern = Pattern.compile("org/radargun/stages/(.*)Stage.class");
         for(;;) {
            ZipEntry entry = inputStream.getNextEntry();
            if (entry == null) break;
            Matcher m = pattern.matcher(entry.getName());
            if (m.matches()) {
               stageNames.add(m.group(1));
            }
         }
      } catch (IOException e) {
         System.err.println("Failed to open " + stageJarFile);
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      return stageNames;
   }

   private static Element createSequence(Document doc, Element parentComplex) {
      Element sequence = doc.createElementNS(NS_XS, XS_SEQUENCE);
      parentComplex.appendChild(sequence);
      return sequence;
   }

   private static Element createChoice(Document doc, Element productsSequence, int minOccurs, int maxOccurs) {
      Element choice = doc.createElementNS(NS_XS, XS_CHOICE);
      choice.setAttribute(XS_MIN_OCCURS, String.valueOf(minOccurs));
      choice.setAttribute(XS_MAX_OCCURS, maxOccurs < 0 ? "unbounded" : String.valueOf(maxOccurs));
      productsSequence.appendChild(choice);
      return choice;
   }

   private static Element createComplexElement(Document doc, Element parentSequence, String name, int minOccurs, int maxOccurs) {
      Element element = doc.createElementNS(NS_XS, XS_ELEMENT);
      element.setAttribute(XS_NAME, name);
      if (minOccurs >= 0) element.setAttribute(XS_MIN_OCCURS, String.valueOf(minOccurs));
      element.setAttribute(XS_MAX_OCCURS, maxOccurs < 0 ? "unbounded" : String.valueOf(maxOccurs));
      parentSequence.appendChild(element);
      Element complex = doc.createElementNS(NS_XS, XS_COMPLEX_TYPE);
      element.appendChild(complex);
      return complex;
   }

   private static Element createComplexType(Document doc, Element parentSequence, String name, String extended, boolean isAbstract) {
      Element typeElement = doc.createElementNS(NS_XS, XS_COMPLEX_TYPE);
      if (isAbstract) typeElement.setAttribute(XS_ABSTRACT, "true");
      typeElement.setAttribute(XS_NAME, name);
      parentSequence.appendChild(typeElement);
      if (extended == null) {
         return typeElement;
      } else {
         Element complexContent = doc.createElementNS(NS_XS, XS_COMPLEX_CONTENT);
         typeElement.appendChild(complexContent);
         Element extension = doc.createElementNS(NS_XS, XS_EXTENSION);
         extension.setAttribute(XS_BASE, extended);
         complexContent.appendChild(extension);
         return extension;
      }
   }

   private static Element createReference(Document doc, Element parent, String name, String type) {
      Element reference = doc.createElementNS(NS_XS, XS_ELEMENT);
      reference.setAttribute(XS_NAME, name);
      reference.setAttribute(XS_TYPE, type);
      parent.appendChild(reference);
      return reference;
   }

   private static void addAttribute(Document doc, Element complexTypeElement, String name) {
      Element attribute = doc.createElementNS(NS_XS, XS_ATTRIBUTE);
      attribute.setAttribute(XS_NAME, name);
      attribute.setAttribute(XS_TYPE, "string");
      complexTypeElement.appendChild(attribute);
   }

   public static void main(String[] args) {
      if (args.length < 1) System.err.println("No schema location directory specified!");
      if (args.length < 2) System.err.println("No jar file with stages specified!");
      stageJarFile = args[1];
      for (int i = 2; i < args.length; ++i) products.add(args[i]);
      generate(args[0]);
   }
}
