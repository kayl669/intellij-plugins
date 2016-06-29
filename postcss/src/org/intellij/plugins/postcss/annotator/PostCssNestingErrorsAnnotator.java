package org.intellij.plugins.postcss.annotator;

import com.intellij.css.util.CssPsiUtil;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.css.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.plugins.postcss.PostCssBundle;
import org.intellij.plugins.postcss.PostCssLanguage;
import org.intellij.plugins.postcss.actions.AddAmpersandToSelectorQuickFix;
import org.intellij.plugins.postcss.actions.AddAtRuleNestToSelectorQuickFix;
import org.intellij.plugins.postcss.actions.DeleteAmpersandQuickFix;
import org.intellij.plugins.postcss.actions.DeleteAtRuleNestQuickFix;
import org.intellij.plugins.postcss.psi.PostCssPsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PostCssNestingErrorsAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    final Language stylesheetLanguage = CssPsiUtil.getStylesheetLanguage(element);
    if (stylesheetLanguage == PostCssLanguage.INSTANCE) {
      if (element instanceof CssSelector) {
        CssSelector selector = ((CssSelector)element);
        if (PostCssPsiUtil.isInsideNestedRuleset(selector)) {
          completeAllSelectorsWithoutAmpersand(selector, holder);
        }
        else {
          checkNotContainsNestingSelectors(selector, holder);
        }
      }
      if (element instanceof CssSelectorList) {
        CssSelectorList selectorList = ((CssSelectorList)element);
        if (PostCssPsiUtil.isInsideNestedRuleset(selectorList)) {
          addAtRuleNesting(selectorList, holder);
        }
      }
    }
  }

  private static void completeAllSelectorsWithoutAmpersand(CssSelector selector, AnnotationHolder holder) {
    CssSelectorList selectorList = PsiTreeUtil.getParentOfType(selector, CssSelectorList.class);
    if (selectorList == null) return;
    if (PostCssPsiUtil.isStartWithNest(selectorList)) {
      if (!PostCssPsiUtil.containsAmpersand(selector)) {
        holder.createErrorAnnotation(selector, PostCssBundle.message("annotator.nested.selector.doesnt.have.ampersand.error"));
      }
    }
    else if (!PostCssPsiUtil.containsAmpersand(selector)) {
      Annotation annotation =
        holder.createErrorAnnotation(selector, PostCssBundle.message("annotator.nested.selector.doesnt.starts.with.ampersand.error"));
      annotation.registerFix(new AddAmpersandToSelectorQuickFix(selector));
    }
  }

  private static void addAtRuleNesting(CssSelectorList list, AnnotationHolder holder) {
    if (PostCssPsiUtil.isStartWithNest(list)) return;
    boolean everySelectorHasAmpersand = Arrays.stream(list.getSelectors()).allMatch(PostCssPsiUtil::containsAmpersand);
    boolean everySelectorStartsWithAmpersand = Arrays.stream(list.getSelectors()).allMatch(PostCssPsiUtil::startsWithAmpersand);
    if (everySelectorHasAmpersand && !everySelectorStartsWithAmpersand) {
      Annotation annotation =
        holder.createErrorAnnotation(list, PostCssBundle.message("annotator.nested.selector.list.doesnt.have.nest.at.rule.error"));
      annotation.registerFix(new AddAtRuleNestToSelectorQuickFix(list));
    }
  }

  private static void checkNotContainsNestingSelectors(CssSelector selector, AnnotationHolder holder) {
    CssSimpleSelector[] directNests =
      Arrays.stream(selector.getSimpleSelectors()).filter(PostCssPsiUtil::isAmpersand).toArray(CssSimpleSelector[]::new);
    if (directNests != null) {
      for (CssSimpleSelector directNest : directNests) {
        Annotation annotation = holder.createErrorAnnotation(directNest, PostCssBundle
          .message("annotator.normal.selector.contains.direct.nesting.selector"));
        annotation.registerFix(new DeleteAmpersandQuickFix(directNest));
      }
    }
    CssSimpleSelector[] nests = Arrays.stream(selector.getSimpleSelectors()).filter(PostCssPsiUtil::isNest).toArray(CssSimpleSelector[]::new);
    if (nests != null) {
      for (CssSimpleSelector nest : nests) {
        Annotation annotation = holder.createErrorAnnotation(nest, PostCssBundle
          .message("annotator.normal.selector.contains.nest"));
        annotation.registerFix(new DeleteAtRuleNestQuickFix(nest));
      }
    }
  }
}