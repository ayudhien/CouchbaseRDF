package ch.unifr.couchbaseRDF;

//import nl.vu.jena.sparql.engine.iterator.QueryIterBlockTriples;

import org.openjena.atlas.logging.Log;


import ch.unifr.couchbaseRDF.reorder.ReorderHeuristics;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterBlockTriples;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import com.hp.hpl.jena.sparql.mgt.Explain;
import com.hp.hpl.jena.sparql.util.Utils;

public class CouchbaseStageGenerator implements StageGenerator {
	
	public CouchbaseStageGenerator() {
	}

	public QueryIterator execute(BasicPattern pattern, 
                                 QueryIterator input,
                                 ExecutionContext execCxt)
    {
        if ( input == null )
            Log.fatal(this, "Null input to "+Utils.classShortName(this.getClass())) ;

        Graph graph = execCxt.getActiveGraph() ; 

        // Choose reorder transformation and execution strategy.
        
        final ReorderTransformation reorder ;
        final StageGenerator executor ;
        
		reorder = reorderBasicStats(graph);
		executor = executeInline;

        return execute(pattern, reorder, executor, input, execCxt) ;
    }

    protected QueryIterator execute(BasicPattern pattern,
                                    ReorderTransformation reorder,
                                    StageGenerator execution, 
                                    QueryIterator input,
                                    ExecutionContext execCxt)
    {
        
        Explain.explain(pattern, execCxt.getContext()) ;
        
        if ( reorder != null )
        {
            pattern = reorder.reorder(pattern) ;
            Explain.explain("Reorder", pattern, execCxt.getContext()) ;
        }

        return execution.execute(pattern, input, execCxt) ; 
    }
    
    private static StageGenerator executeInline = new StageGenerator() {
        public QueryIterator execute(BasicPattern pattern, QueryIterator input, ExecutionContext execCxt)
        {
                return QueryIterBlockTriples.create(input, pattern, execCxt) ;
        }} ;
        
    // ---- Reorder policies 

    // Uses Jena's statistics handler.
    private static ReorderTransformation reorderBasicStats(Graph graph)
    {
        return new ReorderHeuristics();
    }

    /** Use the inline BGP matcher */ 
    public static QueryIterator executeInline(BasicPattern pattern, QueryIterator input, ExecutionContext execCxt)
    {
        return QueryIterBlockTriples.create(input, pattern, execCxt) ;
    }

   
}
