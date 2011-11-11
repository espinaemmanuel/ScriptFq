package org.apache.solr.scriptfq;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestScriptFunctionQuery extends SolrTestCaseJ4 {
	
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig-scriptfq.xml", "schema-scriptfq.xml", getFile("org/apache/solr/scriptfq").getAbsolutePath());
  }

  
  public void dofunc(String func, double val) throws Exception {

	String sval = Float.toString((float)val);

    assertQ(req("fl", "*,score", "defType","func", "fq","id:1", "q",func),
            "//float[@name='score']='" + sval + "'");
  }

  @Test
  public void testFuncs() throws Exception {
    assertU(adoc("id", "1", "foo_pd", "9.34"));
    assertU(adoc("id", "2", "foo_pd", "3.44"));
    assertU(adoc("id", "3", "foo_pd", "5.87"));
    assertU(adoc("id", "4", "foo_pd", "1.78"));
    assertU(adoc("id", "5", "foo_pd", "3.00"));
    assertU(commit());    

    dofunc("js(pi)", 3.14);
    dofunc("js(e)", 2.7182);
    dofunc("js(getVal)", 9.34);
    dofunc("js(duplicate)", 18.68);
  }

  
}
