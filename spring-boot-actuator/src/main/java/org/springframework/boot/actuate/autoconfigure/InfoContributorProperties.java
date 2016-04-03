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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for core info contributors.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties("management.info")
public class InfoContributorProperties {

	private final Build build = new Build();

	private final Git git = new Git();

	public Build getBuild() {
		return this.build;
	}

	public Git getGit() {
		return this.git;
	}

	public static class Build {

		/**
		 * Mode to use to expose build information.
		 */
		private BuildInfoContributor.Mode mode = BuildInfoContributor.Mode.SIMPLE;

		public BuildInfoContributor.Mode getMode() {
			return this.mode;
		}

		public void setMode(BuildInfoContributor.Mode mode) {
			this.mode = mode;
		}

	}

	public static class Git {

		/**
		 * Mode to use to expose git information.
		 */
		private GitInfoContributor.Mode mode = GitInfoContributor.Mode.SIMPLE;

		public GitInfoContributor.Mode getMode() {
			return this.mode;
		}

		public void setMode(GitInfoContributor.Mode mode) {
			this.mode = mode;
		}

	}

}
