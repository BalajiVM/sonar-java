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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Rule(
  key = "S1192",
  name = "String literals should not be duplicated",
  priority = Priority.MINOR,
  tags = {"design"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleLinearWithOffsetRemediation(coeff = "2min", offset = "2min", effortToFixDescription = "per duplicate instance")
public class StringLiteralDuplicatedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_THRESHOLD = 3;

  private static final Integer MINIMAL_LITERAL_LENGTH = 7;
  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private final Multimap<String, LiteralTree> occurrences = ArrayListMultimap.create();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    occurrences.clear();
    scan(context.getTree());
    for (String entry : occurrences.keySet()) {
      Collection<LiteralTree> literalTrees = occurrences.get(entry);
      int literalOccurence = literalTrees.size();
      if (literalOccurence >= threshold) {
        List<JavaFileScannerContext.Location> flow = new ArrayList<>();
        for (Tree element : literalTrees) {
          flow.add(new JavaFileScannerContext.Location("Duplication", element));
        }
        context.reportIssue(
          this,
          Iterables.getFirst(literalTrees, null),
          "Define a constant instead of duplicating this literal " + entry + " " + literalOccurence + " times.",
          flow,
          literalOccurence);
      }
    }
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      String literal = tree.value();
      if (literal.length() >= MINIMAL_LITERAL_LENGTH) {
        occurrences.put(literal, tree);
      }
    }
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    //Ignore literals within annotation
  }
}
