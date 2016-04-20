/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} to adapt annotations marked with
 * {@link PropertyMapping @PropertyMapping}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class AnnotationsPropertySource extends EnumerablePropertySource<Class<?>> {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final Map<String, Object> properties;

	public AnnotationsPropertySource(Class<?> source) {
		this("Annotations", source);
	}

	public AnnotationsPropertySource(String name, Class<?> source) {
		super(name, source);
		this.properties = getProperties(source);
	}

	private Map<String, Object> getProperties(Class<?> source) {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		collectProperties(source, properties);
		return Collections.unmodifiableMap(properties);
	}

	private void collectProperties(Class<?> source, Map<String, Object> properties) {
		if (source != null) {
			for (Annotation annotation : source.getDeclaredAnnotations()) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
					PropertyMapping typeMapping = AnnotationUtils.getAnnotation(
							annotation.annotationType(), PropertyMapping.class);
					for (Method attribute : annotation.annotationType()
							.getDeclaredMethods()) {
						collectProperties(annotation, attribute, typeMapping, properties);
					}
				}
			}
			collectProperties(source.getSuperclass(), properties);
		}
	}

	private void collectProperties(Annotation annotation, Method attribute,
			PropertyMapping typeMapping, Map<String, Object> properties) {
		PropertyMapping attributeMapping = AnnotationUtils.getAnnotation(attribute,
				PropertyMapping.class);
		if (isMapped(typeMapping, attributeMapping)) {
			String name = getName(typeMapping, attributeMapping, attribute);
			ReflectionUtils.makeAccessible(attribute);
			Object value = ReflectionUtils.invokeMethod(attribute, annotation);
			putProperties(name, value, properties);
		}
	}

	private boolean isMapped(PropertyMapping typeMapping,
			PropertyMapping attributeMapping) {
		if (attributeMapping != null) {
			return attributeMapping.map();
		}
		return (typeMapping != null && typeMapping.map());
	}

	private String getName(PropertyMapping typeMapping, PropertyMapping attributeMapping,
			Method attribute) {
		String prefix = (typeMapping == null ? "" : typeMapping.value());
		String name = (attributeMapping == null ? "" : attributeMapping.value());
		if (!StringUtils.hasText(name)) {
			name = toKebabCase(attribute.getName());
		}
		return dotAppend(prefix, name);
	}

	private String toKebabCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result,
					matcher.group(1) + '-' + StringUtils.uncapitalize(matcher.group(2)));
		}
		matcher.appendTail(result);
		return result.toString().toLowerCase();
	}

	private String dotAppend(String prefix, String postfix) {
		if (StringUtils.hasText(prefix)) {
			return (prefix.endsWith(".") ? prefix + postfix : prefix + "." + postfix);
		}
		return postfix;
	}

	private void putProperties(String name, Object value,
			Map<String, Object> properties) {
		if (ObjectUtils.isArray(value)) {
			Object[] array = ObjectUtils.toObjectArray(value);
			for (int i = 0; i < array.length; i++) {
				properties.put(name + "[" + i + "]", array[i]);
			}
		}
		else {
			properties.put(name, value);
		}
	}

	@Override
	public boolean containsProperty(String name) {
		return this.properties.containsKey(name);
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.properties.keySet());
	}

	public boolean isEmpty() {
		return this.properties.isEmpty();
	}

	@Override
	public int hashCode() {
		return this.properties.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.properties.equals(((AnnotationsPropertySource) obj).properties);
	}

}
