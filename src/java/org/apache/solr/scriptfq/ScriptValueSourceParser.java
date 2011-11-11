package org.apache.solr.scriptfq;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.ValueSourceParser;

public class ScriptValueSourceParser extends ValueSourceParser {
	
	private String script;
	private ScriptEngineManager sm = new ScriptEngineManager();
	private ScriptEngine engine;
	private Invocable invocable;
	
	private boolean evaluated = false;
	
	@Override
	public ValueSource parse(FunctionQParser fp) throws ParseException {
		String function = fp.parseArg();
		if (!evaluated){
			try {
				engine.eval(script);
				evaluated = true;
			} catch (ScriptException e) {
				throw new ParseException("Could not evaluate the script", e);
			}
		}
		
		return new ScriptValueSource(invocable, function);
	}
	
	@Override
	public void init(NamedList args) {
		script = (String) args.get("script");
		engine = sm.getEngineByName("jav8");
		invocable = (Invocable) engine;
		

	}

}
