/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Add 'value' element to provide a way to supply contextual information to clients
 *******************************************************************************/
package org.eclipse.pde.api.tools.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes, interfaces, annotations, enums, methods and fields tagged with this
 * annotation are declaring they are not to be referenced at all by clients. For
 * example a method tagged with this annotation should not be called by clients.
 * If this annotation appears anywhere other than classes, interfaces,
 * annotations, enums, methods and fields it will be ignored.
 *
 * @since 1.0
 */
@Documented
@Target(value = {
		ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
		ElementType.FIELD })
@Retention(RetentionPolicy.CLASS)
public @interface NoReference {

	/**
	 * A message to provide contextual information to clients about why this
	 * annotations is applied.
	 *
	 * @since 1.3
	 */
	String value() default "This element is not intended to be referenced by clients.";

}
