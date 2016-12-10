package example;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.gmu.swe.datadep.HeapWalker;
import edu.gmu.swe.datadep.StaticFieldDependency;

public class ExampleDepDetectionITCase {

	static Example bar = null;

	@BeforeClass
	public static void setupWhitelist(){
		HeapWalker.clearWhitelist();
		HeapWalker.addToWhitelist("example");
		System.out.println("Manually forcing whitelist");
	}


	@Test
	public void runTests() throws Exception {
		doTest1();
		doTest2();
		doTest3();
		doTest4();
	}
	
	public void doTest1(){
		Example baz = new Example(false, Example.Ex.BAR);
		System.out.println("test1 "+HeapWalker.walkAndFindDependencies("test1", "test1"));
	}

	public void doTest2(){
		bar = new Example(false, Example.Ex.BAR);
		System.out.println("test2 "+HeapWalker.walkAndFindDependencies("test2", "test2"));

	}

	public void doTest3(){
		bar.isBar();
		bar.bar();
		System.out.println("test3 "+HeapWalker.walkAndFindDependencies("test3", "test3"));
	}

	public void doTest4(){
		assertTrue(bar.getBaz().equals(Example.Ex.BAR));
		bar.setBaz(Example.Ex.FOO);
		assertTrue(bar.getBaz().equals(Example.Ex.FOO));

		List<StaticFieldDependency> deps = HeapWalker.walkAndFindDependencies("test4", "test4");
		System.out.println(deps);
		StaticFieldDependency dep = deps.get(0);
		System.out.println("Actual: " + dep.depGen);
		assertTrue("Wrong Generation",dep.depGen == 2);
		System.out.println("test4 "+deps);
	}

//	static int i = 1;
//
//	@After
//	public void teardown(){
//		i++;
//		System.out.println(HeapWalker.walkAndFindDependencies("test"+i, "test"+i));
//	}

	@AfterClass
	public static void resetHeapWalker()
	{
		HeapWalker.resetAllState();
	}

	static class Example{

		public enum Ex{
			FOO, BAR
		}

		private boolean bar;
		private Ex baz;

		public Example(boolean bar, Ex baz){
			this.bar = bar;
			this.baz = baz;
		}

		public Ex getBaz(){
			return this.baz;
		}

		public void setBaz(Ex baz){
			this.baz = baz;
		}

		public boolean isBar(){
			return bar;
		}

		public void bar(){
			bar = true;
		}
	}

}
