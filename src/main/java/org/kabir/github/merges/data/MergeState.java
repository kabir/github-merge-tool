/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.kabir.github.merges.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MergeState {

    NEW("new", true),
    TESTING("testing", true),
    CLOSED("closed", false),
    MERGED("merged", false);

    private final String state;
    private final boolean open;

    private static final Map<String, MergeState> MAP;
    static {
        HashMap<String, MergeState> map = new HashMap<>();
        for (MergeState ms : MergeState.values()) {
            map.put(ms.state, ms);
        }
        MAP = Collections.unmodifiableMap(map);
    }


    private MergeState(String state, boolean open) {
        this.state = state;
        this.open = open;
    }

    public String getStateString() {
        return state;
    }

    public boolean isOpen() {
        return open;
    }


    public static MergeState fromString(String state){
        return MAP.get(state);
    }
}
