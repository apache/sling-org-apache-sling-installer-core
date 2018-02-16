/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.core.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FileDataStoreTest {
    @Test
    public void testDigestFromDictionary() {
        Dictionary<String,Object> dict1 = new Hashtable<>();
        List<String> l1 = new ArrayList<>();
        l1.add("x");
        l1.add("y");
        dict1.put("la", l1);
        dict1.put("foo", new int[] {3, 2, 7});
        String dig1 = FileDataStore.computeDigest(dict1);

        Dictionary<String,Object> dict2 = new Hashtable<>();
        List<String> l2 = new LinkedList<>();
        l2.add("x");
        l2.add("y");
        dict2.put("la", l2);
        dict2.put("foo", new int[] {3, 2, 7});
        String dig2 = FileDataStore.computeDigest(dict2);
        assertEquals(dig1, dig2);
    }

    @Test
    public void testDigestFromDictionaryNegative1() {
        Dictionary<String,Object> dict1 = new Hashtable<>();
        List<String> l1 = new ArrayList<>();
        l1.add("y");
        l1.add("x");
        dict1.put("la", l1);
        dict1.put("foo", new int[] {3, 2, 7});
        String dig1 = FileDataStore.computeDigest(dict1);

        Dictionary<String,Object> dict2 = new Hashtable<>();
        List<String> l2 = new LinkedList<>();
        l2.add("x");
        l2.add("y");
        dict2.put("la", l2);
        dict2.put("foo", new int[] {3, 2, 7});
        String dig2 = FileDataStore.computeDigest(dict2);
        assertNotEquals(dig1, dig2);
    }

    @Test
    public void testDigestFromDictionaryNegative2() {
        Dictionary<String,Object> dict1 = new Hashtable<>();
        List<String> l1 = new ArrayList<>();
        l1.add("x");
        l1.add("y");
        dict1.put("la", l1);
        dict1.put("foo", new int[] {7, 2, 3});
        String dig1 = FileDataStore.computeDigest(dict1);

        Dictionary<String,Object> dict2 = new Hashtable<>();
        List<String> l2 = new LinkedList<>();
        l2.add("x");
        l2.add("y");
        dict2.put("la", l2);
        dict2.put("foo", new int[] {3, 2, 7});
        String dig2 = FileDataStore.computeDigest(dict2);
        assertNotEquals(dig1, dig2);
    }
}
