/**
 *  Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.ndatasource.core;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.wso2.carbon.ndatasource.core.utils.DataSourceUtils;

/**
 * This class represents properties related to JNDI mapping of a data source.
 */
@XmlRootElement (name = "jndiConfig")
public class JNDIConfig {
	
	private String name;
	
	private List<EnvEntry> environment;

	public void setName(String name) {
		this.name = name;
	}

	public void setEnvironment(List<EnvEntry> environment) {
		this.environment = environment;
	}

	@XmlElement (name = "name", required = true, nillable = false)
	public String getName() {
		return name;
	}

	@XmlElementWrapper (name = "environment", nillable = false)
	@XmlElement (name = "property")
	public List<EnvEntry> getEnvironment() {
		return environment;
	}
	
	@XmlTransient
	public Hashtable<String, String> getHashtableEnv() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		if (this.getEnvironment() != null) {
		    for (EnvEntry entry : this.getEnvironment()) {
			    env.put(entry.getName(), entry.getValue());
		    }
		}
		return env;
	}
	
	public JNDIConfig copy() {
		JNDIConfig result = new JNDIConfig();
		result.setName(this.getName());
		List<EnvEntry> envEntries = new ArrayList<JNDIConfig.EnvEntry>();
		if (this.getEnvironment() != null) {
			EnvEntry tmpEnv;
		    for (EnvEntry entry : this.getEnvironment()) {
		    	tmpEnv = new EnvEntry();
		    	tmpEnv.setName(entry.getName());
		    	tmpEnv.setValue(entry.getValue());
		    	envEntries.add(tmpEnv);
		    }
		}
		return result;
	}
	
	@Override
	public boolean equals(Object rhs) {
		if (!(rhs instanceof JNDIConfig)) {
			return false;
		}
		JNDIConfig jc = (JNDIConfig) rhs;
		if (!DataSourceUtils.nullAllowEquals(jc.getName(), this.getName())) {
			return false;
		}
		if (!DataSourceUtils.nullAllowEquals(jc.getHashtableEnv(), this.getHashtableEnv())) {
			return false;
		}
		return true;
	}
	
	@XmlRootElement (name = "property")
	public static class EnvEntry {
		
		private String name;
		
		private boolean encrypted = true;
		
		private String value;

		@XmlAttribute (name = "encrypted")
		public boolean isEncrypted() {
			return encrypted;
		}

		public void setEncrypted(boolean encrypted) {
			this.encrypted = encrypted;
		}

		@XmlAttribute (name = "name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@XmlValue
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
	}
	
}
