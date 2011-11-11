package org.apache.solr.scriptfq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptException;

import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.queries.function.DocValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.DoubleDocValues;
import org.apache.lucene.queries.function.valuesource.DoubleFieldSource;
import org.apache.lucene.search.cache.CachedArrayCreator;
import org.apache.lucene.search.cache.DoubleValuesCreator;

public class ScriptValueSource extends ValueSource {
	
	private Invocable invocable;
	private String function;

	public ScriptValueSource(Invocable invocable, String function) {
		this.invocable = invocable;
		this.function = function;
	}
	
	public class DocumentProxy {
		
		private Map context;
		private AtomicReaderContext readerContext;
		private Map<String,DocValues> doubleValuesCache = new HashMap<String, DocValues>();
		private int currentDoc = 0;
				
		private DoubleFieldSource createDoubleFieldSource(String field){
			return new DoubleFieldSource( new DoubleValuesCreator( field, null, CachedArrayCreator.CACHE_VALUES_AND_BITS ) );
		}
		
		private DocValues getDoubleDocValues(String field) throws IOException{
			DocValues docValues = doubleValuesCache.get(field);
			if(docValues == null){
				//TODO: field does not exists?
				try {
				docValues = createDoubleFieldSource(field).getValues(context, readerContext);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				doubleValuesCache.put(field, docValues);
			}
			
			return docValues;
		}
		
		public DocumentProxy(Map context, AtomicReaderContext readerContext) {
			this.context = context;
			this.readerContext = readerContext;
		}
		
		protected void setCurrentDoc(int doc){
			currentDoc = doc;
		}

		public double doubleVal(String field){
			try {
				return getDoubleDocValues(field).doubleVal(this.currentDoc);
			} catch (IOException e) {
				// TODO Handle
				return Double.NaN;
			}
		}
		
	}
	
	@Override
	public DocValues getValues(Map context, AtomicReaderContext readerContext)
			throws IOException {
		
		final DocumentProxy docProxy = new DocumentProxy(context, readerContext);
		
		return new DoubleDocValues(this) {
			
			@Override
			public double doubleVal(int doc) {
				Object val;
				try {
					docProxy.setCurrentDoc(doc);
					val = invocable.invokeFunction(function, docProxy);
					
					if(val instanceof Number){
						return ((Number)val).doubleValue();
					} else {
						//TODO: Handle this as an exception?
						return Double.NaN;
					}				
					
					//TODO: Handle the exceptions better
				} catch (ScriptException e) {
					return Double.NaN;
				} catch (NoSuchMethodException e) {
					return Double.NaN;
				}
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return null;
	}

}
