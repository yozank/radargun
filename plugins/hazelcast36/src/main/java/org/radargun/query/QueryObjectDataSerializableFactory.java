package org.radargun.query;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class QueryObjectDataSerializableFactory implements DataSerializableFactory {
   @Override
   public IdentifiedDataSerializable create(int i) {
      if (i == 1) {
         return new TextObject();
      }

      return null;
   }
}
