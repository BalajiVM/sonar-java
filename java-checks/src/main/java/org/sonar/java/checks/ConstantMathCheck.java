/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;
import java.util.List;

@Rule(
  key = "S2185",
  name = "Silly math should not be performed",
  priority = Priority.MAJOR,
  tags = {"clumsy"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
public class ConstantMathCheck extends SubscriptionBaseVisitor implements JavaFileScanner {

  private static final String ABS = "abs";
  private static final String CEIL = "ceil";
  private static final String DOUBLE = "double";
  private static final String FLOAT = "float";
  private static final String FLOOR = "floor";
  private static final String MATH_PACKAGE_NAME = "java.lang.Math";
  private static final String ROUND = "round";

  private static final MethodInvocationMatcherCollection CONSTANT_WITH_LITERAL_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ABS).addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ABS).addParameter(FLOAT),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ABS).addParameter("int"),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ABS).addParameter("long")
    );

  private static final MethodInvocationMatcherCollection TRUNCATION_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(CEIL).addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(CEIL).addParameter(FLOAT),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(FLOOR).addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(FLOOR).addParameter(FLOAT),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("rint").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ROUND).addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name(ROUND).addParameter(FLOAT)
    );

  private static final MethodInvocationMatcherCollection CONSTANT_WITH_ZERO_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("atan2").addParameter(DOUBLE).addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("cos").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("cosh").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("expm1").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("sin").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("sinh").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("tan").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("tanh").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("toRadians").addParameter(DOUBLE)
    );

  private static final MethodInvocationMatcherCollection CONSTANT_WITH_ZERO_OR_ONE_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("acos").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("asin").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("atan").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("cbrt").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("exp").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("log").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("log10").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("sqrt").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("toDegrees").addParameter(DOUBLE),
    MethodMatcher.create().typeDefinition(MATH_PACKAGE_NAME).name("exp").addParameter(DOUBLE)
    );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.REMAINDER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.REMAINDER)) {
        BinaryExpressionTree remainderTree = (BinaryExpressionTree) tree;
        if (isIntegralOne(remainderTree.rightOperand())) {
          addIssue(tree, "Remove this computation of % 1, which always evaluates to zero.");
        }
      } else {
        MethodInvocationTree methodTree = (MethodInvocationTree) tree;
        if (isConstantWithLiteral(methodTree) || isTruncation(methodTree) || isConstantWithZero(methodTree) || isConstantWithZeroOrOne(methodTree)) {
          addIssue(tree, String.format("Remove this silly call to \"Math.%s\"", methodTree.symbol().name()));
        }
      }
    }
  }

  private static boolean isTruncation(MethodInvocationTree methodTree) {
    return TRUNCATION_METHODS.anyMatch(methodTree) && isCastFromIntegralToFloating(ExpressionsHelper.skipParentheses(methodTree.arguments().get(0)));
  }

  private static boolean isConstantWithLiteral(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_LITERAL_METHODS.anyMatch(methodTree) && isConstant(methodTree.arguments().get(0));
  }

  private static boolean isConstantWithZero(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_METHODS.anyMatch(methodTree) && isFloatingZero(methodTree.arguments().get(0));
  }

  private static boolean isConstantWithZeroOrOne(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_OR_ONE_METHODS.anyMatch(methodTree) && isFloatingZeroOrOne(methodTree.arguments().get(0));
  }

  private static boolean isCastFromIntegralToFloating(ExpressionTree tree) {
    Type resultType = tree.symbolType();
    // explicit cast
    if (tree.is(Tree.Kind.TYPE_CAST) && isIntegral(getInnerType(((TypeCastTree) tree).expression())) && (resultType.is(DOUBLE) || resultType.is(FLOAT))) {
      return true;
    }
    // implicit cast
    return isIntegral(resultType);
  }

  private static boolean isConstant(ExpressionTree tree) {
    return getInnerExpression(tree).is(Tree.Kind.CHAR_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL);
  }

  private static boolean isIntegral(Type type) {
    return type.isPrimitive() && !type.is(DOUBLE) && !type.is(FLOAT);
  }

  private static boolean isIntegralOne(ExpressionTree tree) {
    Long value = LiteralUtils.longLiteralValue(tree);
    return value != null && value == 1;
  }

  private static ExpressionTree getInnerExpression(ExpressionTree tree) {
    ExpressionTree result = ExpressionsHelper.skipParentheses(tree);
    while (result.is(Tree.Kind.TYPE_CAST)) {
      result = ExpressionsHelper.skipParentheses(((TypeCastTree) result).expression());
    }
    return result;
  }

  private static Type getInnerType(ExpressionTree tree) {
    return getInnerExpression(tree).symbolType();
  }

  private static boolean isFloatingZero(ExpressionTree tree) {
    Integer value = getFloatingZeroOrOne(tree);
    return value != null && value == 0;
  }

  private static boolean isFloatingZeroOrOne(ExpressionTree tree) {
    return getFloatingZeroOrOne(tree) != null;
  }

  @CheckForNull
  private static Integer getFloatingZeroOrOne(ExpressionTree tree) {
    ExpressionTree expressionTree = ExpressionsHelper.skipParentheses(tree);
    if (expressionTree.is(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL)) {
      String value = ((LiteralTree) expressionTree).value();
      if ("0.0".equals(value) || "0.0d".equalsIgnoreCase(value) || "0.0f".equalsIgnoreCase(value)) {
        return 0;
      } else if ("1.0".equals(value) || "1.0d".equalsIgnoreCase(value) || "1.0f".equalsIgnoreCase(value)) {
        return 1;
      }
    }
    return null;
  }

}
