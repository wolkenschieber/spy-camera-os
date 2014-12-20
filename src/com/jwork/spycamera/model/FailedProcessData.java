/* ========================================================================
 * Copyright 2013 Jimmy Halim
 * Licensed under the Creative Commons Attribution-NonCommercial 3.0 license 
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://creativecommons.org/licenses/by-nc/3.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */
package com.jwork.spycamera.model;

/**
 * @author Jimmy Halim
 */
public class FailedProcessData {
	
	private Throwable throwable;
	private String title;
	private String report;
	private String exit;
	private boolean forceExit;
	private String flag;
	
	public FailedProcessData(final Throwable ex, String message, String captionSendReport, String captionExit
			, final boolean forceExit, final String prefsString) {
		this.throwable = ex;
		this.title = message;
		this.report = captionSendReport;
		this.exit = captionExit;
		this.forceExit = forceExit;
		this.flag = prefsString;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getReport() {
		return report;
	}

	public void setReport(String report) {
		this.report = report;
	}

	public String getExit() {
		return exit;
	}

	public void setExit(String exit) {
		this.exit = exit;
	}

	public boolean isForceExit() {
		return forceExit;
	}

	public void setForceExit(boolean forceExit) {
		this.forceExit = forceExit;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

}
