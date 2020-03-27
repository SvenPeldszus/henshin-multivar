///**
// * 
// */
//package org.eclipse.emf.henshin.variability.ocltrans;
//
//import java.io.IOException;
//import java.net.URL;
//import java.text.SimpleDateFormat;
//import java.util.Collections;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Map;
//
//import org.eclipse.core.resources.IFile;
//import org.eclipse.core.resources.IResource;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.FileLocator;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.emf.common.util.BasicEList;
//import org.eclipse.emf.common.util.EList;
//import org.eclipse.emf.common.util.TreeIterator;
//import org.eclipse.emf.common.util.URI;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.EPackage;
//import org.eclipse.emf.ecore.EcorePackage;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.resource.ResourceSet;
//import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
//import org.eclipse.emf.ecore.util.EcoreUtil;
//import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
//import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
//import org.eclipse.emf.henshin.interpreter.EGraph;
//import org.eclipse.emf.henshin.interpreter.Engine;
//import org.eclipse.emf.henshin.interpreter.UnitApplication;
//import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
//import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
//import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
//import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
//import org.eclipse.emf.henshin.model.Module;
//import org.eclipse.emf.henshin.model.Unit;
//import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
//import org.eclipse.emf.henshin.trace.Trace;
//import org.eclipse.ocl.examples.pivot.BooleanLiteralExp;
//import org.eclipse.ocl.examples.pivot.CallExp;
//import org.eclipse.ocl.examples.pivot.Class;
//import org.eclipse.ocl.examples.pivot.Constraint;
//import org.eclipse.ocl.examples.pivot.ExpressionInOCL;
//import org.eclipse.ocl.examples.pivot.IfExp;
//import org.eclipse.ocl.examples.pivot.IntegerLiteralExp;
//import org.eclipse.ocl.examples.pivot.Iteration;
//import org.eclipse.ocl.examples.pivot.IteratorExp;
//import org.eclipse.ocl.examples.pivot.LiteralExp;
//import org.eclipse.ocl.examples.pivot.OCLExpression;
//import org.eclipse.ocl.examples.pivot.Operation;
//import org.eclipse.ocl.examples.pivot.OperationCallExp;
//import org.eclipse.ocl.examples.pivot.PivotFactory;
//import org.eclipse.ocl.examples.pivot.PivotPackage;
//import org.eclipse.ocl.examples.pivot.RealLiteralExp;
//import org.eclipse.ocl.examples.pivot.Root;
//import org.eclipse.ocl.examples.pivot.StringLiteralExp;
//import org.eclipse.ocl.examples.pivot.UnlimitedNaturalLiteralExp;
//import org.eclipse.ui.internal.decorators.FullTextDecoratorRunnable;
//
//import GraphConstraint.GraphConstraintPackage;
//import GraphConstraint.NestedGraphConstraint;
//
///**
// * @author Thorsten
// *
// */
//public class TranslatorCopy {
//	
//	private static final String ECOREOCLAS = "ecore.oclas";
//	private static final String TRACEFILE = ".trace";
//	private static final String TRACEROOT = "traceroot";
//	private static final String AND = "and";
//	private static final String NOT = "not";
//	private static final String OR = "or";
//	private static final String IMPLIES = "implies";
//	private static final String NGC = "ngc";
//	private static final String GRAPHCONSTRAINT = ".graphconstraint";
//	private static final String GRAPHCONSTRAINTS = "/graphconstraints_";
//	private static final String INVARIANT = "invariant";
//	private static final String INIT_UNIT = "init";
//	private static final String MAIN_UNIT = "main";
//	private static final String PATH = "files";
//	
//	private EList<Constraint> invariants = null;	
//	private UnitApplication initUnitApp = null;
//	private EPackage metamodel = null;
//	private Root root = null;
//	private EGraphImpl graph;
//	private Unit mainUnit;
//	private String oclasUri;
//	private String ecoreUri;
//	private String resultPath;
//	private String rulePath;
//	private String exampleID;
//	private String ruleFileName;
//	private Engine  engine;
//	
//
//	public TranslatorCopy(String rulePath, String ruleFileName, String oclasUri, String ecoreUri, String resultPath, String exampleID) {
//		this.rulePath = rulePath;
//		this.ruleFileName = ruleFileName;
//		this.oclasUri = oclasUri;
//		this.ecoreUri = ecoreUri;
//		this.resultPath = resultPath;
//		this.exampleID = exampleID;
//		invariants = new BasicEList<Constraint>();
//		init();
//	}
//
//	private void init() {
//			URI uriOclAS = URI.createFileURI(oclasUri);
//			URI uriEcore = URI.createFileURI(ecoreUri);
//			if (uriOclAS != null && uriEcore != null) {
//				// Load the input models and the corresponding invariants
//				PivotPackage.eINSTANCE.eClass();
//				EcorePackage.eINSTANCE.eClass();
//				GraphConstraintPackage.eINSTANCE.eClass();
//			    Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
//			    Map<String, Object> m = reg.getExtensionToFactoryMap();
//			    m.put("oclas", new XMIResourceFactoryImpl());
//			    m.put("ecore", new EcoreResourceFactoryImpl());
//			    ResourceSet resSet = new ResourceSetImpl();
//			    Resource resourceOclAS = resSet.getResource(uriOclAS, true);
//			    root = (Root) resourceOclAS.getContents().get(0);
//			    // Preparations
//			    prepareOCLModel(root);
////			    savePreparedOCLModel();
//			    // Load Ecore for meta/type model references
//				Resource resourceEcore = resSet.getResource(uriEcore, true);
//				metamodel = (EPackage) resourceEcore.getContents().get(0);					
//				
//				// Load the transformation module
//				HenshinResourceSet resourceSet = new HenshinResourceSet(rulePath);
//				Module module = resourceSet.getModule(ruleFileName, false);
//							
//				// Initialize the Henshin interpreter
//				graph = new EGraphImpl(root);
//				graph.addGraph(metamodel);
//				engine = new EngineImpl();			
//				Unit initUnit = module.getUnit(INIT_UNIT);
//				initUnitApp = new UnitApplicationImpl(engine, graph, initUnit, null);
//				mainUnit = module.getUnit(MAIN_UNIT);		
//			}
//	}
//
//	public void savePreparedOCLModel() {
//		Date date = new GregorianCalendar().getTime();
//		String path = resultPath + GRAPHCONSTRAINTS + exampleID;
//		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
//		resourceSet.saveEObject(root, root.getName().concat(ECOREOCLAS));
//	}
//
//	public static void prepareOCLModel(Root root) {
//		TreeIterator<EObject> iter = root.eAllContents();
//		System.out.println("Preparing");
//		while(iter.hasNext()) {
//			EObject eObject = iter.next();
////			if (eObject instanceof Class) {
////				invariants.addAll(((Class) eObject).getOwnedInvariant());
////			}
//			if (eObject instanceof OperationCallExp) {
//				OperationCallExp operationCallExp = (OperationCallExp) eObject;		
//				Operation op = operationCallExp.getReferredOperation();
//				System.out.println(op);
//				// put operation name to operation call 
//				operationCallExp.setName(op.getName());
//				// remove referred operation since not needed anymore 
//				operationCallExp.setReferredOperation(null);
//			}
//			if (eObject instanceof IteratorExp) {
//				IteratorExp iteratorExp = (IteratorExp) eObject;
//				Iteration iteration = iteratorExp.getReferredIteration();
//				// put iteration name to iteration call 
//				System.out.println(iteration.getName());
//				iteratorExp.setName(iteration.getName());
//				// remove referred iteration since not needed anymore 
//				iteratorExp.setReferredIteration(null);
//			}
//		}
//		iter = root.eAllContents();
//		while(iter.hasNext()) {
//			EObject eObject = iter.next();
//			if (eObject instanceof OperationCallExp) {
//				OperationCallExp operationCallExp = (OperationCallExp) eObject;
//				// refactor boolean operation 'implies'
//				if (operationCallExp.getName().equals(IMPLIES)) {
//					refactorImpliesOperation(operationCallExp);
//				}
//			}
//			// refactor IF expression
//			if (eObject instanceof IfExp) {
//				IfExp ifExp = (IfExp) eObject;
//				refactorIfExpression(ifExp);
//			}	
//			// refactor literals
//			if (eObject instanceof LiteralExp) {
//				LiteralExp literalExp = (LiteralExp) eObject;
//				refactorLiteralExpression(literalExp);
//			}
//		}
//		System.out.println("Finished preparing");
//	}
//
//	private static void refactorLiteralExpression(LiteralExp literalExp) {
//		if (literalExp instanceof StringLiteralExp) {
//			StringLiteralExp exp = (StringLiteralExp) literalExp;
//			exp.setName(exp.getStringSymbol());
//		}
//		if (literalExp instanceof BooleanLiteralExp) {
//			BooleanLiteralExp exp = (BooleanLiteralExp) literalExp; 
//			exp.setName(Boolean.toString(exp.isBooleanSymbol()));
//		}
//		if (literalExp instanceof RealLiteralExp) {
//			RealLiteralExp exp = (RealLiteralExp) literalExp;
//			exp.setName(Double.toString(exp.getRealSymbol().doubleValue()));
//		}
//		if (literalExp instanceof UnlimitedNaturalLiteralExp) {
//			UnlimitedNaturalLiteralExp exp = (UnlimitedNaturalLiteralExp) literalExp;
//			exp.setName(Integer.toString(exp.getUnlimitedNaturalSymbol().intValue()));
//		}
//		if (literalExp instanceof IntegerLiteralExp) {
//			IntegerLiteralExp exp = (IntegerLiteralExp) literalExp;
//			exp.setName(Integer.toString(exp.getIntegerSymbol().intValue()));
//		}		
//	}
//
//	private static void refactorImpliesOperation(OperationCallExp operationCallExp) {
//		operationCallExp.setName(OR);
//		OCLExpression oldSourceExpression = operationCallExp.getSource();
//		OperationCallExp newSourceExpression = PivotFactory.eINSTANCE.createOperationCallExp();
//		newSourceExpression.setName(NOT);
//		operationCallExp.setSource(newSourceExpression);
//		newSourceExpression.setSource(oldSourceExpression);
//	}
//
//	private static void refactorIfExpression(IfExp ifExp) {
//		// remember and copy OCL expressions
//		OCLExpression conditionExpression = ifExp.getCondition();
//		OCLExpression conditionExpressioCopy = EcoreUtil.copy(conditionExpression);
//		OCLExpression thenExpression = ifExp.getThenExpression();
//		OCLExpression elseExpression = ifExp.getElseExpression();
//		ifExp.setCondition(null);
//		ifExp.setThenExpression(null);
//		ifExp.setElseExpression(null);
//		// create the new structure
//		PivotFactory factory = PivotFactory.eINSTANCE;
//		OperationCallExp opCallOR = factory.createOperationCallExp();
//		opCallOR.setName(OR);
//		OperationCallExp opCallANDSource = factory.createOperationCallExp();
//		opCallANDSource.setName(AND);
//		opCallANDSource.setSource(conditionExpression);
//		opCallANDSource.getArgument().add(thenExpression);
//		opCallOR.setSource(opCallANDSource);
//		OperationCallExp opCallANDArgument = factory.createOperationCallExp();
//		opCallANDArgument.setName(AND);
//		OperationCallExp opCallNOT = factory.createOperationCallExp();
//		opCallNOT.setName(NOT);
//		opCallNOT.setSource(conditionExpressioCopy);
//		opCallANDArgument.setSource(opCallNOT);
//		opCallANDArgument.getArgument().add(elseExpression);
//		opCallOR.getArgument().add(opCallANDArgument);
//		// replace if expression by new structure
//		EObject container = ifExp.eContainer();
//		// case 1: is body expression of specification
//		if (container instanceof ExpressionInOCL) {
//			ExpressionInOCL expressionInOCL = (ExpressionInOCL) container;
//			if (expressionInOCL.getBodyExpression() == ifExp) {
//				expressionInOCL.setBodyExpression(opCallOR);
//			}
//		}
//		// case 2: is body of iterator
//		if (container instanceof IteratorExp) {
//			IteratorExp iteratorExp = (IteratorExp) container;
//			if (iteratorExp.getBody() == ifExp) {
//				iteratorExp.setBody(opCallOR);
//			}
//		}
//		// case 3: is source of call expression
//		if (container instanceof CallExp) {
//			CallExp callExp = (CallExp) container;
//			if (callExp.getSource() == ifExp) {
//				callExp.setSource(opCallOR);
//			}
//		}
//		// case 4: is argument of operation call expression
//		if (container instanceof OperationCallExp) {
//			OperationCallExp operationCallExp = (OperationCallExp) container;
//			if (operationCallExp.getArgument().contains(ifExp)) {
//				int index = operationCallExp.getArgument().indexOf(ifExp);
//				operationCallExp.getArgument().add(index, opCallOR);
//			}
//		}
//		if (container instanceof IfExp) {
//			IfExp ifExpContainer = (IfExp) container;
//			// case 5: is condition of if expression
//			if (ifExpContainer.getCondition() == ifExp) {
//				ifExpContainer.setCondition(opCallOR);
//			}
//			// case 6: is then of if expression
//			if (ifExpContainer.getThenExpression() == ifExp) {
//				ifExpContainer.setThenExpression(opCallOR);
//			}
//			// case 7: is else of if expression
//			if (ifExpContainer.getElseExpression() == ifExp) {
//				ifExpContainer.setElseExpression(opCallOR);
//			}
//		}
//		
//	}
//
//	public void saveNestedGraphConstraint(Date date, NestedGraphConstraint ngc, Trace trace) {	
//		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
//		String path = resultPath+GRAPHCONSTRAINTS + sdf.format(date);
//		HenshinResourceSet resourceSet = new HenshinResourceSet(path);
//		resourceSet.saveEObject(ngc, ngc.getName().concat(GRAPHCONSTRAINT)+".xmi");
//		resourceSet.saveEObject(root, root.getName().concat(ECOREOCLAS));
//		resourceSet.saveEObject(trace, ngc.getName().concat("1" + TRACEFILE)+".xmi");
//		printTrace(trace);
//		resourceSet.saveEObject(trace, ngc.getName().concat("2" + TRACEFILE)+".xmi");
//	}
//
//	private void printTrace(Trace trace) {
//		trace.getSource().clear();
//		trace.getTarget().clear();
//		if (trace.getName() != null && ! trace.getName().isEmpty()) {
//			System.out.println(trace.getName());
//		}
//		for (Trace subTrace : trace.getSubTraces()) {
//			printTrace(subTrace);
//		}
//	}
//
//	public UnitApplication getInitUnitApp() {
//		return initUnitApp;
//	}
//
//	public EList<Constraint> getInvariants() {
//		return invariants;
//	}
//
//	public EPackage getMetamodel() {
//		return metamodel;
//	}
//
//	public EGraphImpl getGraph() {
//		return graph;
//	}
//
//	public Unit getMainUnit() {
//		return mainUnit;
//	}
//
//	public Engine getEngine() {
//		return engine;
//	}	
//
//}
