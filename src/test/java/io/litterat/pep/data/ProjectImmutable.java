/*
 * Copyright (c) 2020, Live Media Pty. Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.litterat.pep.data;

import io.litterat.pep.Data;
import io.litterat.pep.ToData;
import io.litterat.pep.data.ProjectImmutable.ProjectImmutableData;

public class ProjectImmutable implements ToData<ProjectImmutableData> {

	private final int x;
	private final int y;

	public static class ProjectImmutableData {

		private final int x;
		private final int y;

		@Data
		public ProjectImmutableData(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int x() {
			return x;
		}

		public int y() {
			return y;
		}

	}

	public ProjectImmutable(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Data
	public ProjectImmutable(ProjectImmutableData data) {
		this.x = data.x;
		this.y = data.y;
	}

	@Override
	public ProjectImmutableData toData() {
		return new ProjectImmutableData(x, y);
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

}