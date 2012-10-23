/**
 * Copyright (C) 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.jirm.core.sql;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import co.jirm.core.sql.SqlPlaceholderParser.ParsedSql;
import co.jirm.core.sql.SqlPlaceholderParser.PlaceHolderType;
import co.jirm.core.util.ResourceUtils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;


public class SqlPlaceholderParserTest {

	@Before
	public void setUp() throws Exception {}
	
	
	@Test
	public void testBasic() throws Exception {
	
		PlainSql sql = PlainSql.fromResource(getClass(), "select-test-bean.sql")
				.set("name", "Adam")
				.set("limit", 1);
		assertEquals(ImmutableList.<Object>of("Adam", 1), sql.mergedParameters());
		assertEquals(
				"SELECT * from test_bean\n" + 
				"WHERE stringProp like ? \n" + 
				"LIMIT ? ", sql.getSql());
	}
	
	@Test
	public void test() throws Exception {
		String sql = ResourceUtils.getClasspathResourceAsString(getClass(), "search-recruiting.sql");
		ParsedSql psql = SqlPlaceholderParser.parseSql(sql);
		assertEquals("SELECT \n" + 
				"c.id, c.name, c.tags, c.category, c.description, \n" + 
				"c.division, c.experience_level as \"experienceLevel\", \n" + 
				"c.locations, c.type, c.parent_id as \"parentId\", \n" + 
				"g.latitude as \"latitude\", g.longitude as \"longitude\"\n" + 
				"FROM campaign c\n" + 
				"LEFT OUTER JOIN \n" + 
				"	(SELECT DISTINCT cg.campaign, geo.latitude, geo.longitude from campaign_geo cg\n" + 
				"	INNER JOIN geo geo on geo.id = cg.geo \n" + 
				"	WHERE geo.latitude IS NOT NULL AND geo.longitude IS NOT NULL AND cg.createts < ? \n" + 
				"	) g on g.campaign = c.id\n" + 
				"WHERE c.type = 'JOBPAGE' AND c.createts < ? \n" + 
				"ORDER BY c.createts ASC, c.id, g.latitude, g.longitude\n" + 
				"LIMIT ? \n" + 
				"OFFSET ? \n" + 
				"", psql.getResultSql());
		assertEquals(4, psql.getPlaceHolders().size());
		assertEquals(PlaceHolderType.POSITION, psql.getPlaceHolders().get(0).getType());
	}
	
	@Test
	public void testName() throws Exception {
		String sql = ResourceUtils.getClasspathResourceAsString(getClass(), "search-recruiting-name.sql");
		ParsedSql psql = SqlPlaceholderParser.parseSql(sql);
		assertEquals("SELECT \n" + 
				"c.id, c.name, c.tags, c.category, c.description, \n" + 
				"c.division, c.experience_level as \"experienceLevel\", \n" + 
				"c.locations, c.type, c.parent_id as \"parentId\", \n" + 
				"g.latitude as \"latitude\", g.longitude as \"longitude\"\n" + 
				"FROM campaign c\n" + 
				"LEFT OUTER JOIN \n" + 
				"	(SELECT DISTINCT cg.campaign, geo.latitude, geo.longitude from campaign_geo cg\n" + 
				"	INNER JOIN geo geo on geo.id = cg.geo \n" + 
				"	WHERE geo.latitude IS NOT NULL AND geo.longitude IS NOT NULL AND cg.createts < ? \n" + 
				"	) g on g.campaign = c.id\n" + 
				"WHERE c.type = 'JOBPAGE' AND c.createts < ? \n" + 
				"ORDER BY c.createts ASC, c.id, g.latitude, g.longitude\n" + 
				"LIMIT ? \n" + 
				"OFFSET ? \n" + 
				"", psql.getResultSql());
		assertEquals(4, psql.getPlaceHolders().size());
		assertEquals(PlaceHolderType.NAME, psql.getPlaceHolders().get(0).getType());
		assertEquals("now", psql.getPlaceHolders().get(0).asName().getName());
		
	}
	
	@Test
	public void testNameParameters() throws Exception {
		PlainSql sql = PlainSql.parse(ResourceUtils.getClasspathResourceAsString(getClass(), "search-recruiting-name.sql"));
		sql.with("1", "2", "3", "4");
		assertEquals(ImmutableList.<Object>of("1","2","3", "4"), sql.mergedParameters());
		sql = PlainSql.parse(ResourceUtils.getClasspathResourceAsString(getClass(), "search-recruiting-name.sql"));
		sql
			.set("now", "1")
			.set("limit", "10")
			.set("offset", "100");
		assertEquals(ImmutableList.<Object>of("1","1","10", "100"), sql.mergedParameters());
	}
	
	@Test
	public void testPerformance() throws Exception {
		Stopwatch sw = new Stopwatch().start();
		String sql = ResourceUtils.getClasspathResourceAsString(getClass(), "search-recruiting-name.sql");
		for (int i = 0; i < 300000; i++) {
			PlainSql.parse(sql);
		}
		
		assertTrue("Should be faster",sw.stop().elapsedMillis() < 3000);
	}
	
	@Test
	public void testPerformanceFromClasspath() throws Exception {
		Stopwatch sw = new Stopwatch().start();
		for (int i = 0; i < 300000; i++) {
			PlainSql.fromResource(getClass(), "search-recruiting-name.sql");
		}
		
		assertTrue("Should be faster",sw.stop().elapsedMillis() < 3000);
	}
	
	@Test
	public void testApos() throws Exception {
		PlainSql sql = PlainSql.fromResource(getClass(), "select-test-bean.sql");
		String result = sql
				.set("name", "Adam")
				.set("limit", 1).getSql();
		assertEquals(
				"SELECT * from test_bean\n" + 
				"WHERE stringProp like ? \n" + 
				"LIMIT ? ", result);
	}
	
	@Test
	public void testMultiApos() throws Exception {
		int i[] = SqlPlaceholderParser.parseForReplacement("blah = ''''");
		assertArrayEquals(new int[] {7,11}, i);
		String replace = "blah = ' Hey butt hole '' '  ";
		i = SqlPlaceholderParser.parseForReplacement(replace);
		assertArrayEquals(new int[] {7,27}, i);
		assertEquals("' Hey butt hole '' '", replace.substring(i[0], i[1]));
	}

}