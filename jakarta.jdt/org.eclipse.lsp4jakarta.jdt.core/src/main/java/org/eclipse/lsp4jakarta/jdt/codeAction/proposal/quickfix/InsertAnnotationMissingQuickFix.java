/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4jakarta.jdt.codeAction.proposal.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4jakarta.jdt.codeAction.IJavaCodeActionParticipant;
import org.eclipse.lsp4jakarta.jdt.codeAction.JavaCodeActionContext;
import org.eclipse.lsp4jakarta.jdt.codeAction.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4jakarta.jdt.codeAction.proposal.NewAnnotationProposal;

/**
 * QuickFix for inserting annotations.
 * Reused from https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/codeaction/InsertAnnotationMissingQuickFix.java
 *
 * @author Angelo ZERR
 *
 */
public class InsertAnnotationMissingQuickFix implements IJavaCodeActionParticipant {

    private final String[] annotations;

    private final boolean generateOnlyOneCodeAction;

    /**
     * Constructor for insert annotation quick fix.
     *
     * <p>
     * The participant will generate a CodeAction per annotation.
     * </p>
     *
     * @param annotations list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(String... annotations) {
        this(false, annotations);
    }

    /**
     * Constructor for insert annotation quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which insert the list of
     *                                  annotation and false otherwise.
     * @param annotations               list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(boolean generateOnlyOneCodeAction, String... annotations) {
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.annotations = annotations;
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic,
            IProgressMonitor monitor) throws CoreException {
        ASTNode node = context.getCoveredNode();
        IBinding parentType = getBinding(node);
        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();
            insertAnnotations(diagnostic, context, parentType, codeActions);
            return codeActions;
        }
        return null;
    }

    protected static IBinding getBinding(ASTNode node) {
        if (node.getParent() instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.getParent();
            return ((VariableDeclarationFragment) node.getParent()).resolveBinding();
        }
        if (node.getParent() instanceof MethodDeclaration) {
        	MethodDeclaration methodDecl = (MethodDeclaration) node.getParent();
            return methodDecl.resolveBinding();
        }
        return Bindings.getBindingOfParentType(node);
    }

    protected String[] getAnnotations() {
        return this.annotations;
    }

    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context, IBinding parentType,
            List<CodeAction> codeActions) throws CoreException {
        if (generateOnlyOneCodeAction) {
            insertAnnotation(diagnostic, context, parentType, codeActions, annotations);
        } else {
            for (String annotation : annotations) {
                insertAnnotation(diagnostic, context, parentType, codeActions, annotation);
            }
        }
    }

    protected static void insertAnnotation(Diagnostic diagnostic, JavaCodeActionContext context, IBinding parentType,
            List<CodeAction> codeActions, String... annotations) throws CoreException {
        // Insert the annotation and the proper import by using JDT Core Manipulation
        // API
        String name = getLabel(annotations);
        ChangeCorrectionProposal proposal = new NewAnnotationProposal(name, context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, annotations);
        // Convert the proposal to LSP4J CodeAction
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    private static String getLabel(String[] annotations) {
        StringBuilder name = new StringBuilder("Insert ");
        for (int i = 0; i < annotations.length; i++) {
            String annotation = annotations[i];
            String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
            if (i > 0) {
                name.append(", ");
            }
            name.append("@");
            name.append(annotationName);
        }
        return name.toString();
    }

}