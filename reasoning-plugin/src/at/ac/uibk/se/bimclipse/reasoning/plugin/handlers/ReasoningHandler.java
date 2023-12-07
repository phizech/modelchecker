package at.ac.uibk.se.bimclipse.reasoning.plugin.handlers;

import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import at.ac.uibk.se.bimclipse.reasoning.plugin.ui.components.ReasoningDialog;

public class ReasoningHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ReasoningDialog dialog = getClickedFilePath(event)
				.map((modelPath) -> new ReasoningDialog(HandlerUtil.getActiveShell(event), modelPath))
				.orElse(new ReasoningDialog(HandlerUtil.getActiveShell(event)));
		
		dialog.open();
		
		return null;
	}

	private Optional<String> getClickedFilePath(ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			ISelectionService service = window.getSelectionService();
			IStructuredSelection structured = (IStructuredSelection) service.getSelection();
			IFile file = (IFile) structured.getFirstElement();
			return Optional.of(file.getLocation().toOSString());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
