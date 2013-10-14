/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.model;

import java.util.List;

/**
 * Modifiers.
 *
 * JLS 8.1.1, 8.3.1, 8.4.3, 8.5.1, 8.8.3, 9.1.1, 9.7
 *
 * @since Java 1.3
 */
public interface ModifiersTree extends Tree {

  /**
   * @since Java 1.5
   */
  List<? extends AnnotationTree> annotations();

  List<Modifier> modifiers();

}