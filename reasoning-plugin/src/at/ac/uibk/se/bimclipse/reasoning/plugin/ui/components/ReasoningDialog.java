package at.ac.uibk.se.bimclipse.reasoning.plugin.ui.components;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.rulesys.Rule;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import at.ac.uibk.se.bimclipse.reasoning.engine.CheckExecutionEngine;
import at.ac.uibk.se.bimclipse.reasoning.engine.events.LoggingEventHandler;
import at.ac.uibk.se.bimclipse.reasoning.engine.specification.Specification;

public class ReasoningDialog extends Dialog {

	// global UI elements
	private Button startButton;
	private ProgressBar progressBar;
	private Label currentEngineStatus;
	private Tree solutionTree;
	private Shell errorMessageDialog;
	
	private final Runnable engineExecutionRunnable = new Runnable() {
		
		@Override
		public void run() {
			new CheckExecutionEngine(new ReasoningEngineEventHandler()).execute(modelPath, specificationPath);
			startButton.getDisplay().asyncExec(() -> startButton.setEnabled(true));
		}
		
	};
	
	// state
	private String modelPath;
	private String specificationPath;
	private Thread engineExecutionThread = new Thread(engineExecutionRunnable);
	private Map<RDFNode, List<QuerySolution>> solutionBuffer = new HashMap<>();
 	
	public ReasoningDialog(Shell shell) {
		super(shell);
	}
	
	public ReasoningDialog(Shell shell, String modelPath) {
		this(shell);
		
		this.modelPath = modelPath;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		// define the dialog layout
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		container.setLayout(layout);
		
		// create the model selector
		Text modelPathSearch = new Text(container, SWT.SEARCH);
		modelPathSearch.setMessage("Select the model...");
		if (modelPath != null) {
			modelPathSearch.setText(modelPath);
		}
		GridData mpcLayout = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		mpcLayout.horizontalSpan = 3;
		modelPathSearch.setLayoutData(mpcLayout);
		Button modelPickerButton = new Button(container, SWT.NONE);
		modelPickerButton.setText("Select...");
		modelPickerButton.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				System.out.println("modelPickerButton::selected");
				
				FileDialog dialog = new FileDialog(new Shell());
				dialog.setText("Import a model");
				dialog.setFilterExtensions(new String[] { "*.ifc;*.rdf;*.ifc;*.owl;*.ttl;*.xml" });
				
				String path = dialog.open();
				
				// handle invalid path
				if (path == null) {
					return;
				}
				
				modelPath = path;
				modelPathSearch.setText(modelPath);
				refreshActionButtons();
			}
			
		});
		
		// create the specification selector
		Text specificationPathSearch = new Text(container, SWT.SEARCH);
		specificationPathSearch.setMessage(specificationPath == null ? "Select a specification..." : specificationPath);
		GridData spcLayout = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		spcLayout.horizontalSpan = 3;
		specificationPathSearch.setLayoutData(mpcLayout);
		Button specificationPickerButton = new Button(container, SWT.NONE);
		specificationPickerButton.setText("Select...");
		specificationPickerButton.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				System.out.println("specificationPickerButton::selected");
				
				FileDialog dialog = new FileDialog(new Shell());
				dialog.setText("Import a specification");
				dialog.setFilterExtensions(new String[] { "*.zip" });
				
				String path = dialog.open();
				
				// handle invalid path
				if (path == null) {
					return;
				}
				
				specificationPath = path;
				specificationPathSearch.setText(specificationPath);
				refreshActionButtons();
			}
			
		});
		
		// create the action section
		Composite actionSection = new Composite(container, SWT.NONE);
		actionSection.setLayout(new FillLayout(SWT.HORIZONTAL));
		startButton = new Button(actionSection, SWT.NONE);
		startButton.setText("Start");
		startButton.addListener(SWT.Selection, (event) -> onStartButtonClick());
		
		// create the status section
		progressBar = new ProgressBar(container, SWT.HORIZONTAL);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		currentEngineStatus = new Label(container, SWT.NONE);
		currentEngineStatus.setText("Idle...");
		currentEngineStatus.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// create the solution section
		solutionTree = new Tree(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		solutionTree.setHeaderVisible(true);
		GridData treeLayoutData = new GridData(GridData.FILL_HORIZONTAL);
		treeLayoutData.heightHint = 300;
		treeLayoutData.horizontalSpan = 4;
		solutionTree.setLayoutData(treeLayoutData);
	    TreeColumn column1 = new TreeColumn(solutionTree, SWT.LEFT);
	    column1.setText("Checks");
	    column1.setWidth(250);
	    TreeColumn column2 = new TreeColumn(solutionTree, SWT.LEFT);
	    column2.setText("Subject");
	    column2.setWidth(200);
	    TreeColumn column3 = new TreeColumn(solutionTree, SWT.LEFT);
	    column3.setText("Result");
	    column3.setWidth(150);
		
	    // set initial state
		refreshActionButtons();
		progressBar.setVisible(false);
		currentEngineStatus.setVisible(false);
		container.pack();
		
		return container;
	}
		
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", true) ;
	}

	private class ReasoningEngineEventHandler extends LoggingEventHandler {

		@Override
		public void afterCheckExecution(Model finalModel) {
			super.afterCheckExecution(finalModel);
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(80);
			});
		}

		@Override
		public void afterModelEnrichment(Model enrichedModel) {
			super.afterModelEnrichment(enrichedModel);
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(50);
			});
		}

		@Override
		public void afterModelImport(Model baseModel) {
			super.afterModelImport(baseModel);
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(30);
			});
		}

		@Override
		public void afterReasoningEngineDone(List<QuerySolution> solutions) {
			super.afterReasoningEngineDone(solutions);
			progressBar.getDisplay().syncExec(() -> {
				progressBar.setVisible(false);
				progressBar.setSelection(100);
			});
			currentEngineStatus.getDisplay().syncExec(() -> {
				currentEngineStatus.setVisible(false);
				currentEngineStatus.setText("done");
			});
			
			for (QuerySolution solution : solutions) {
				RDFNode check = solution.get("check");
				solutionBuffer.putIfAbsent(check, new ArrayList<>());
				solutionBuffer.get(check).add(solution);
			}
			
			solutionTree.getDisplay().asyncExec(() -> {
				for (Entry<RDFNode, List<QuerySolution>> check : solutionBuffer.entrySet()) {
					TreeItem checkTreeItem = new TreeItem(solutionTree, SWT.NONE);
					checkTreeItem.setText(new String[] { check.getKey().toString() });
					for (QuerySolution checkSolution : check.getValue()) {
						TreeItem checkSolutionTreeItem = new TreeItem(checkTreeItem, SWT.NONE);
						checkSolutionTreeItem.setText(new String[] {
								checkSolution.get("subjectId").toString(),
								checkSolution.get("subject").toString(),
								checkSolution.get("checkResult").toString()
						});
					}
				}
				
				if (solutionTree.getItemCount() > 0) {
					solutionTree.setSelection(solutionTree.getItem(0));
				}
			});
		}

		@Override
		public void afterResultExtraction(List<QuerySolution> solutions) {
			super.afterResultExtraction(solutions);
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(90);
			});
		}

		@Override
		public void afterSpecificationImport(Specification specification) {
			super.afterSpecificationImport(specification);
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(20);
			});
		}

		@Override
		public void beforeCheckExecution(Model currentModel, List<Rule> checks) {
			super.beforeCheckExecution(currentModel, checks);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setText("executing checks...");
			});
			
		}

		@Override
		public void beforeModelEnrichment(Model currentModel, List<Model> ontologies, List<Rule> rules,
				List<Query> queries) {
			super.beforeModelEnrichment(currentModel, ontologies, rules, queries);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setText("performing model enrichment...");
			});
		}

		@Override
		public void beforeModelImport(String modelPath) {
			super.beforeModelImport(modelPath);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setText("importing model...");
			});
		}

		@Override
		public void beforeReasoningEngineStarted(String modelPath, String specificationPath) {
			super.beforeReasoningEngineStarted(modelPath, specificationPath);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setVisible(true);
				currentEngineStatus.setText("starting query engine...");
			});
			progressBar.getDisplay().asyncExec(() -> {
				progressBar.setSelection(0);
				progressBar.setVisible(true);
			});
		}
		
		@Override
		public void beforeResultExtraction(Model model) {
			super.beforeResultExtraction(model);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setText("extracting results...");
			});
		}

		@Override
		public void beforeSpecificationImport(String specificationPath) {
			super.beforeSpecificationImport(specificationPath);
			currentEngineStatus.getDisplay().asyncExec(() -> {
				currentEngineStatus.setText("importing specification...");
			});
		}
		
		@Override
		public void onFatalException(Exception reason) {
			super.onFatalException(reason);
			
			StringWriter stringWriter = new StringWriter();
			reason.printStackTrace(new PrintWriter(stringWriter));
			String stackTrace = stringWriter.toString();
			
			displayError(stackTrace);
		}
		
		@Override
		public void onFatalError(Error reason) {
			super.onFatalError(reason);
			
			StringWriter stringWriter = new StringWriter();
			reason.printStackTrace(new PrintWriter(stringWriter));
			String stackTrace = stringWriter.toString();
			
			displayError(stackTrace);
		}
		
		private void displayError(String message) {
			Display.getDefault().syncExec(() -> {
				progressBar.setSelection(100);
				currentEngineStatus.setText("fatal error");
				refreshActionButtons();
			});
			Display.getDefault().syncExec(() -> {
				errorMessageDialog = new Shell(ReasoningDialog.this.getShell());
				errorMessageDialog.setLayout(new GridLayout(1, false));
				
				errorMessageDialog.setText("Fatal Error");
				
				Text text = new Text(errorMessageDialog, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
				GridData layout = new GridData(GridData.FILL_BOTH);
				layout.heightHint = 600;
				layout.widthHint = 600;
				text.setLayoutData(layout);
				
				text.setText(message);
				
				errorMessageDialog.pack();
				errorMessageDialog.open();
			});
		}
		
	}
	
	private void refreshActionButtons() {
		startButton.setEnabled(modelPath != null && specificationPath != null && !engineExecutionThread.isAlive());
	}

	private void onStartButtonClick() {
		// clean up previous results
		solutionBuffer = new HashMap<>();
		solutionTree.clearAll(true);
		solutionTree.setItemCount(0);
		
		// create and start a new execution
		engineExecutionThread = new Thread(engineExecutionRunnable);
		engineExecutionThread.start();
		
		// refresh the user interface
		refreshActionButtons();
	}
	
}
