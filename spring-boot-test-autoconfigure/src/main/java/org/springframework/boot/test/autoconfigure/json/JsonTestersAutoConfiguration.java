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

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.json.AbstractJsonMarshalTester;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Auto-configuration for Json testers.
 *
 * @author Phillip Webb
 * @see AutoConfigureJsonTesters
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass(name = "org.assertj.core.api.Assert")
@ConditionalOnProperty("spring.test.jsontesters.enabled")
public class JsonTestersAutoConfiguration {

	@Bean
	public static JsonMarshalTestersBeanPostProcessor jsonMarshalTestersBeanPostProcessor() {
		return new JsonMarshalTestersBeanPostProcessor();
	}

	@Bean
	@Scope("prototype")
	public FactoryBean<BasicJsonTester> BasicJsonTesterFactoryBean() {
		return new JsonTesterFactoryBean<BasicJsonTester, Void>(BasicJsonTester.class,
				null);
	}

	@ConditionalOnClass(ObjectMapper.class)
	private static class JacksonJsonTestersConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnBean(ObjectMapper.class)
		public FactoryBean<JacksonTester<?>> jacksonTesterFactoryBean(
				ObjectMapper mapper) {
			return new JsonTesterFactoryBean<JacksonTester<?>, ObjectMapper>(
					JacksonTester.class, mapper);
		}

	}

	@ConditionalOnClass(Gson.class)
	private static class GsonJsonTestersConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnBean(Gson.class)
		public FactoryBean<GsonTester<?>> gsonTesterFactoryBean(Gson gson) {
			return new JsonTesterFactoryBean<GsonTester<?>, Gson>(GsonTester.class, gson);
		}

	}

	/**
	 * {@link FactoryBean} used to create JSON Tester instances.
	 */
	private static class JsonTesterFactoryBean<T, M> implements FactoryBean<T> {

		private final Class<?> objectType;

		private final M marshaller;

		JsonTesterFactoryBean(Class<?> objectType, M marshaller) {
			this.objectType = objectType;
			this.marshaller = marshaller;

		}

		@Override
		public boolean isSingleton() {
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getObject() throws Exception {
			if (this.marshaller == null) {
				Constructor<?> constructor = this.objectType.getDeclaredConstructor();
				ReflectionUtils.makeAccessible(constructor);
				return (T) BeanUtils.instantiateClass(constructor);
			}
			Constructor<?>[] constructors = this.objectType.getDeclaredConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterTypes().length == 1
						&& constructor.getParameterTypes()[0]
								.isInstance(this.marshaller)) {
					ReflectionUtils.makeAccessible(constructor);
					return (T) BeanUtils.instantiateClass(constructor, this.marshaller);
				}
			}
			throw new IllegalStateException(
					this.objectType + " does not have a usable constructor");
		}

		@Override
		public Class<?> getObjectType() {
			return this.objectType;
		}

	}

	/**
	 * {@link BeanPostProcessor} used to initialize JSON testers.
	 */
	private static class JsonMarshalTestersBeanPostProcessor
			extends InstantiationAwareBeanPostProcessorAdapter {

		@Override
		public Object postProcessAfterInitialization(final Object bean, String beanName)
				throws BeansException {

			ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {

				@Override
				public void doWith(Field field)
						throws IllegalArgumentException, IllegalAccessException {
					processFiled(bean, field);
				}

			});
			return bean;
		}

		private void processFiled(Object bean, Field field) {
			if (AbstractJsonMarshalTester.class.isAssignableFrom(field.getType())
					|| BasicJsonTester.class.isAssignableFrom(field.getType())) {
				ResolvableType type = ResolvableType.forField(field).getGeneric();
				ReflectionUtils.makeAccessible(field);
				Object tester = ReflectionUtils.getField(field, bean);
				if (tester != null) {
					ReflectionTestUtils.invokeMethod(tester, "initialize",
							bean.getClass(), type);
				}
			}
		}

	}

}
