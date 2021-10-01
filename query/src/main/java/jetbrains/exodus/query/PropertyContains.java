/**
 * Copyright 2010 - 2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.query;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.query.metadata.ModelMetaData;

import static jetbrains.exodus.query.Utils.safe_equals;

public class PropertyContains extends NodeBase {

    private final String name;
    private final String contains;
    private final boolean ignoreCase;

    public PropertyContains(String name, String contains, boolean ignoreCase) {
        this.name = name;
        this.contains = contains;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Iterable<Entity> instantiate(String entityType, QueryEngine queryEngine, ModelMetaData metaData, InstantiateContext context) {
        queryEngine.assertOperational();
        return queryEngine.getPersistentStore().
            getAndCheckCurrentTransaction().findContaining(entityType, name, contains, ignoreCase);
    }

    @Override
    public NodeBase getClone() {
        return new PropertyContains(name, contains, ignoreCase);
    }

    @Override
    public void optimize(Sorts sorts, OptimizationPlan rules) {
        if (contains == null || contains.length() == 0) {
            final NodeBase parent = getParent();
            parent.replaceChild(this, NodeFactory.all());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        checkWildcard(obj);
        if (!(obj instanceof PropertyContains)) {
            return false;
        }
        PropertyContains right = (PropertyContains) obj;
        return safe_equals(name, right.name) && safe_equals(contains, right.contains) && ignoreCase == right.ignoreCase;
    }

    @Override
    public String toString(String prefix) {
        return super.toString(prefix) + '(' + name + ' ' + contains + ") ";
    }

    @Override
    public StringBuilder getHandle(StringBuilder sb) {
        return super.getHandle(sb).append('(').append(name).append(' ').append(contains).append(')');
    }

    @Override
    public String getSimpleName() {
        return "pc";
    }
}
