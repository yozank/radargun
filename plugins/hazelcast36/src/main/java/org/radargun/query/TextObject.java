package org.radargun.query;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;


/**
 * Simple object containing one string. See {@link org.radargun.stages.cache.generators.TextObjectGenerator}
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TextObject implements IdentifiedDataSerializable {
   private String text;

   public TextObject() {}

   public TextObject(String text) {
      this.text = text;
   }

   public String getText() {
      return text;
   }

   @Override
   public String toString() {
      return "TextObject{" + text + '}';
   }

   @Override
   public int getFactoryId() {
      return 1;
   }

   @Override
   public int getId() {
      return 1;
   }

   @Override
   public void writeData(ObjectDataOutput objectDataOutput) throws IOException {
      objectDataOutput.writeUTF(text);
   }

   @Override
   public void readData(ObjectDataInput objectDataInput) throws IOException {
      text = objectDataInput.readUTF();
   }
}
