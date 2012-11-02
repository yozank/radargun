/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.radargun.cachewrappers;

import org.jgroups.JChannel;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.protocols.TP;
import org.jgroups.stack.ProtocolStack;
import org.radargun.features.Partitionable;
import org.radargun.protocols.SLAVE_PARTITION;

import java.util.List;
import java.util.Set;

public class InfinispanPartitionableWrapper extends InfinispanKillableWrapper implements Partitionable {
   
   private static Log log = LogFactory.getLog(InfinispanPartitionableWrapper.class);
   private int mySlaveIndex = -1;
   private Set<Integer> initiallyReachable;
     
   @Override
   protected void preStartCacheManager() {
      if (mySlaveIndex >= 0 && initiallyReachable != null) {
         setMembersInPartition(mySlaveIndex, initiallyReachable);
      }
   }
   
   @Override
   public void setMembersInPartition(int slaveIndex, Set<Integer> members) {
      List<JChannel> channels = getChannels();
      if (channels.size() <= 0) {
         log.warn("No channels found!");
      } else {
         log.trace("Found " + channels.size() + " channels");
      }
      for (JChannel channel : channels) {
         SLAVE_PARTITION partition = (SLAVE_PARTITION) channel.getProtocolStack().findProtocol(SLAVE_PARTITION.class);
         if (partition == null) {
            log.info("No SLAVE_PARTITION protocol found in stack for " + channel.getName() + ", inserting above transport protocol");
            partition = new SLAVE_PARTITION();
            try {
               channel.getProtocolStack().insertProtocol(partition, ProtocolStack.ABOVE, TP.class);
            } catch (Exception e) {
               log.error("Error inserting the SLAVE_PARTITION protocol to stack for " + channel.getName());
               return;
            }         
         }
         partition.setSlaveIndex(slaveIndex);
         partition.setAllowedSlaves(members);
      }
   }

   @Override
   public void setStartWithReachable(int slaveIndex, Set<Integer> members) {
      mySlaveIndex = slaveIndex;
      initiallyReachable = members;
   }
}
