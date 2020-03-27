//package org.eclipse.emf.henshin.variability.ocltrans;
//
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.eclipse.core.resources.IFile;
//import org.eclipse.jface.action.IAction;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.jface.viewers.ISelection;
//import org.eclipse.jface.viewers.StructuredSelection;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Cursor;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.IActionDelegate;
//import org.eclipse.ui.IObjectActionDelegate;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.PlatformUI;
//
//public class TransformAction implements IObjectActionDelegate {
//
//	private Shell shell;
//	private List<IFile> files = null;
//	private IFile ecoreFile = null;
//	private IFile oclasFile = null;
//	
//	/**
//	 * Constructor for Action1.
//	 */
//	public TransformAction() {
//		super();
//	}
//
//	/**
//	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
//	 */
//	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
//		shell = targetPart.getSite().getShell();
//	}
//
//	/**
//	 * @see IActionDelegate#run(IAction)
//	 */
//	public void run(IAction action) {
//		if (checkFiles()) {
//			Cursor oldCursor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getCursor();
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setCursor(new Cursor(null,SWT.CURSOR_WAIT));
//			Translator translator = new Translator(oclasFile, ecoreFile);
//			long timeNeeded = translator.translate();
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setCursor(oldCursor);
//			MessageDialog.openInformation(
//				shell,
//				"Translation: Core OCL invariants 2 Nested graph constraints",
//				"Translation from Core OCL invariants 2 Nested graph constraints successfully executed "
//				+ "(translation time: "	+ timeNeeded + " millisec).");
//		} else {
//			MessageDialog.openInformation(
//					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
//					"Translation: Core OCL invariants 2 Nested graph constraints",
//					"Translation can not be executed on the input.");
//		}
//	}
//
//	private boolean checkFiles() {
//		IFile file1 = files.get(0);
//		IFile file2 = files.get(1);
//		if (! (file1.getName().endsWith(".ecore") || file2.getName().endsWith(".ecore"))) return false;
//		if (! (file1.getName().endsWith(".oclas") || file2.getName().endsWith(".oclas"))) return false;		
//		if (file1.getName().endsWith(".ecore")) {
//			ecoreFile = file1;
//			oclasFile = file2;
//		} else {
//			ecoreFile = file2;
//			oclasFile = file1;
//		}		
//		return true;
//	}
//
//	/**
//	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
//	 */
//	@SuppressWarnings("unchecked")
//	public void selectionChanged(IAction action, ISelection selection) {
//		files = new ArrayList<IFile>();
//		if (selection instanceof StructuredSelection) {
//			StructuredSelection ss = (StructuredSelection) selection;
//			List<Object> objects = ss.toList();
//			for (Object o : objects) {
//				files.add((IFile) o);			
//			}
//		}
//	}
//
//}
