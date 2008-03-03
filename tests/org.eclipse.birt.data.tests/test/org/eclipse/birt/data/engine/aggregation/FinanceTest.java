package org.eclipse.birt.data.engine.aggregation;



import junit.framework.TestCase;

import org.eclipse.birt.aggregation.BuildInAggregationFactory;
import org.eclipse.birt.aggregation.IBuildInAggregation;
import org.eclipse.birt.data.engine.api.aggregation.Accumulator;
import org.eclipse.birt.data.engine.api.aggregation.IAggrFunction;

/**
 *
 * test Finance aggregation function
 */
public class FinanceTest extends TestCase
{

    private BuildInAggregationFactory buildInAggrFactory = new BuildInAggregationFactory();

	/*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testIrr( ) throws Exception
	{
		double b[] = new double[]{-70000d, 12000d, 15000d, 18000d, 21000d,
				26000d};
		double a[] = new double[]{-70000, 12000, 15000};
		double c[] = new double[]{-70000d, 12000d, 15000d, 18000d, 21000d};


		IAggrFunction ag = buildInAggrFactory.getAggregation("irr");
        Accumulator ac = ag.newAccumulator();
        assertEquals(IBuildInAggregation.TOTAL_IRR_FUNC, ag.getName());
      
        assertEquals(IAggrFunction.SUMMARY_AGGR, ag.getType());
        assertEquals(2, ag.getParameterDefn().length);
        assertTrue(!ag.getParameterDefn()[0].isOptional( ));
        assertTrue(!ag.getParameterDefn()[1].isOptional( ));
        
        ac.start();
        for(int i=0; i<b.length; i++)
        {
            ac.onRow(new Object[]{new Double(b[i]), new Double(-0.6)});
        }
        ac.finish();
        assertEquals( 0.0866, ((Double)ac.getValue()).doubleValue(), 0.0001 );
        
        ac.start();
        for(int i=0; i<a.length; i++)
        {
            ac.onRow(new Object[]{new Double(a[i]), new Double(-0.6)});
        }
        ac.finish();
        assertEquals( -0.44, ((Double)ac.getValue()).doubleValue(), 0.01 );
        
        ac.start();
        for(int i=0; i<c.length; i++)
        {
            ac.onRow(new Object[]{new Double(c[i]), new Double(-0.1)});
        }
        ac.finish();
        assertEquals( -0.021244848272999998, ((Double)ac.getValue()).doubleValue(), Double.MIN_VALUE );
        
        ac.start();
        ac.finish();
        assertEquals(null, ac.getValue());
        
        ac.start();
        try
        {
            ac.getValue();
            assertTrue(false);
        }
        catch(RuntimeException e)
        {
            assertTrue(true);
        }

	}

	public void testMirr( ) throws Exception
	{
		//If the value of the guess is far from the correct answer, application
		// can't get the correct result and will report error
		double a[] = new double[]{-120000, 39000, 30000, 21000, 37000, 46000};
		double b[] = new double[]{-120000, 39000, 30000, 21000};
		IAggrFunction ag = buildInAggrFactory.getAggregation("mirr");
        Accumulator ac = ag.newAccumulator();
        assertEquals(IBuildInAggregation.TOTAL_MIRR_FUNC, ag.getName());
      
        assertEquals(IAggrFunction.SUMMARY_AGGR, ag.getType());
        assertEquals(3, ag.getParameterDefn().length);
        assertTrue(!ag.getParameterDefn()[0].isOptional( ));
        assertFalse(!ag.getParameterDefn()[1].isOptional( ));
        assertFalse(!ag.getParameterDefn()[2].isOptional( ));
        
        ac.start();
        for(int i=0; i<a.length; i++)
        {
            ac.onRow(new Object[]{new Double(a[i]), new Double(0.1), new Double(0.14)});
        }
        ac.finish();
        assertEquals( 0.13475911082831482, ((Double)ac.getValue()).doubleValue(), Double.MIN_VALUE );
        
        ac.start();
        for(int i=0; i<b.length; i++)
        {
            ac.onRow(new Object[]{new Double(b[i]), new Double(0.1), new Double(0.12)});
        }
        ac.finish();
        assertEquals( -0.048044655249980806, ((Double)ac.getValue()).doubleValue(), Double.MIN_VALUE );
        
        
        ac.start();
        ac.finish();
        assertEquals(null, ac.getValue());
        
        ac.start();
        try
        {
            ac.getValue();
            assertTrue(false);
        }
        catch(RuntimeException e)
        {
            assertTrue(true);
        }


	}
	
	public void testNpv( ) throws Exception
	{
	    
	    IAggrFunction ag = buildInAggrFactory .getAggregation("npv");
        Accumulator ac = ag.newAccumulator();
        assertEquals(IBuildInAggregation.TOTAL_NPV_FUNC, ag.getName());
   
        assertEquals(IAggrFunction.SUMMARY_AGGR, ag.getType());
        assertEquals(2, ag.getParameterDefn().length);
        assertTrue(!ag.getParameterDefn()[0].isOptional( ));
        assertFalse(!ag.getParameterDefn()[1].isOptional( ));
        
        double a[] = new double[]{-10000, 3000, 4200, 6800};
        
        ac.start();
        for(int i=0; i<a.length; i++)
        {
            ac.onRow(new Object[]{new Double(a[i]), new Double(0.1)});
        }
        ac.finish();
        assertEquals( 1188.4434123352207, ((Double)ac.getValue()).doubleValue(), Double.MIN_VALUE );
        
        ac.start();
        ac.finish();
        assertEquals(null, ac.getValue());
        
        ac.start();
        try
        {
            ac.getValue();
            assertTrue(false);
        }
        catch(RuntimeException e)
        {
            assertTrue(true);
        }
	}
	
	public void testRunningNpv() throws Exception
	{
	    IAggrFunction ag = buildInAggrFactory.getAggregation("runningnpv");
        Accumulator ac = ag.newAccumulator();
        assertEquals(IBuildInAggregation.TOTAL_RUNNINGNPV_FUNC, ag.getName());
 
        assertEquals(IAggrFunction.RUNNING_AGGR, ag.getType());
        assertEquals(2, ag.getParameterDefn().length);
        assertTrue(!ag.getParameterDefn()[0].isOptional( ));
        assertFalse(!ag.getParameterDefn()[1].isOptional( ));
        
        double a[] = new double[]{-10000, 3000, 4200, 6800};
        double b[] = new double[]{-9090.90909090909, -6611.570247933883, -3456.0480841472577, 1188.4434123352207};
        
        ac.start();
        for(int i=0; i<a.length; i++)
        {
            ac.onRow(new Object[]{new Double(a[i]), new Double(0.1), new Double(0.14)});
            assertEquals( b[i], ((Double)ac.getValue()).doubleValue(), Double.MIN_VALUE );
        }
        ac.finish();       
        
        ac.start();
        ac.finish();
        assertEquals(null, ac.getValue());
        

	}

}
