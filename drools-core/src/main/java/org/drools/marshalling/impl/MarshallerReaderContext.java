/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.marshalling.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

import org.drools.common.BaseNode;
import org.drools.common.InternalFactHandle;
import org.drools.common.InternalRuleBase;
import org.drools.common.InternalWorkingMemory;
import org.drools.marshalling.MarshallerFactory;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.marshalling.ObjectMarshallingStrategyStore;
import org.drools.marshalling.impl.ProtobufInputMarshaller.PBActivationsFilter;
import org.drools.reteoo.LeftTuple;
import org.drools.reteoo.RightTuple;
import org.drools.rule.EntryPoint;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeRuntime;
import org.drools.spi.PropagationContext;

public class MarshallerReaderContext extends ObjectInputStream {
    public final MarshallerReaderContext                 stream;
    public final InternalRuleBase                        ruleBase;
    public InternalWorkingMemory                         wm;
    public KnowledgeRuntime                              kruntime;
    public final Map<Integer, BaseNode>                  sinks;

    public Map<Integer, InternalFactHandle>              handles;

    public final Map<RightTupleKey, RightTuple>          rightTuples;
    public final Map<Integer, LeftTuple>                 terminalTupleMap;
    public final PBActivationsFilter                     filter;
    

    public final ObjectMarshallingStrategyStore          resolverStrategyFactory;
    public final Map<Integer, ObjectMarshallingStrategy> usedStrategies;

    public final Map<String, EntryPoint>                 entryPoints;

    public final Map<Integer, TimersInputMarshaller>     readersByInt;

    public final Map<Long, PropagationContext>           propagationContexts;

    public final boolean                                 marshalProcessInstances;
    public final boolean                                 marshalWorkItems;
    public final Environment                             env;
    
    // this is a map to store node memory data indexed by node ID
    public final Map<Integer, Object>                    nodeMemories;

    public MarshallerReaderContext(InputStream stream,
                                   InternalRuleBase ruleBase,
                                   Map<Integer, BaseNode> sinks,
                                   ObjectMarshallingStrategyStore resolverStrategyFactory,
                                   Map<Integer, TimersInputMarshaller> timerReaders,
                                   Environment env) throws IOException {
        this( stream,
              ruleBase,
              sinks,
              resolverStrategyFactory,
              timerReaders,
              true,
              true, 
              env    );
    }

    public MarshallerReaderContext(InputStream stream,
                                   InternalRuleBase ruleBase,
                                   Map<Integer, BaseNode> sinks,
                                   ObjectMarshallingStrategyStore resolverStrategyFactory,
                                   Map<Integer, TimersInputMarshaller> timerReaders,
                                   boolean marshalProcessInstances,
                                   boolean marshalWorkItems, 
                                   Environment env) throws IOException {
        super( stream );
        this.stream = this;
        this.ruleBase = ruleBase;
        this.sinks = sinks;
        
        this.readersByInt = timerReaders;
        
        this.handles = new HashMap<Integer, InternalFactHandle>();
        this.rightTuples = new HashMap<RightTupleKey, RightTuple>();
        this.terminalTupleMap = new HashMap<Integer, LeftTuple>();
        this.filter = new PBActivationsFilter();
        this.entryPoints = new HashMap<String, EntryPoint>();
        this.propagationContexts = new HashMap<Long, PropagationContext>();
        if(resolverStrategyFactory == null){
            ObjectMarshallingStrategy[] strats = (ObjectMarshallingStrategy[])env.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
            if ( strats == null ) {
                strats = new ObjectMarshallingStrategy[] { MarshallerFactory.newSerializeMarshallingStrategy() } ;
            }
            this.resolverStrategyFactory = new ObjectMarshallingStrategyStoreImpl(strats);
        }
        else{
            this.resolverStrategyFactory = resolverStrategyFactory;
        }
        this.usedStrategies = new HashMap<Integer, ObjectMarshallingStrategy>();
        
        this.marshalProcessInstances = marshalProcessInstances;
        this.marshalWorkItems = marshalWorkItems;
        this.env = env;
        
        this.nodeMemories = new HashMap<Integer, Object>();
    }
    
    @Override
    protected Class< ? > resolveClass(ObjectStreamClass desc) throws IOException,
                                                             ClassNotFoundException {
        String name = desc.getName();
        try {
            return Class.forName(name, false, (this.ruleBase == null)?null:this.ruleBase.getRootClassLoader());
        } catch (ClassNotFoundException ex) {
            return super.resolveClass( desc );
        }
    }
}
