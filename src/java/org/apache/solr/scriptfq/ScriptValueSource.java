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
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.DateField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;

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
		private Map<String,DocValues> docValuesCache = new HashMap<String, DocValues>();
		private int currentDoc = 0;
		
		public static final int DOUBLE_VALUE = 0;
		public static final int DATE_VALUE = 0;		
		
		
		private DocValues getDoubleDocValues(String field) throws IOException {
			return getDocValues(field, DOUBLE_VALUE);
		}
		
		private DocValues getDateDocValues(String field) throws IOException {
			return getDocValues(field, DATE_VALUE);
		}
		
		private DocValues getDocValues(String field, int expectedField) throws IOException{
			DocValues docValues = docValuesCache.get(field);
			
			if (docValues == null) {
				SolrIndexSearcher searcher = (SolrIndexSearcher) context.get("searcher");
				SchemaField f = searcher.getSchema().getField(field);			

			    if (expectedField == DATE_VALUE && f.getType().getClass() == DateField.class) {
			      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Can't convert to miliseconds the non-numeric legacy date field: " + field);
			    }
			    ValueSource dateSource = f.getType().getValueSource(f, null);
			    
			    docValues = dateSource.getValues(context, readerContext);
				docValuesCache.put(field, docValues);
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
		
		public Double dateMsVal(String field){
			try {
				double v = getDateDocValues(field).doubleVal(this.currentDoc);
				return v;
			} catch (IOException e) {
				return null;
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
