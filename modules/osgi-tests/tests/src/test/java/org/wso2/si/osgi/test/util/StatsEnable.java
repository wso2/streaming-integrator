/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.si.osgi.test.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * StatsEnable
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaMSF4JServerCodegen",
        date = "2017-11-02T13:49:11.445Z")
public class StatsEnable {
  @JsonProperty("statsEnable")
  private Boolean statsEnable = null;

  public StatsEnable statsEnable(Boolean statsEnable) {
    this.statsEnable = statsEnable;
    return this;
  }

   /**
   * Get statsEnable
   * @return statsEnable
  **/
  @ApiModelProperty(required = true, value = "")
  public Boolean getStatsEnable() {
    return statsEnable;
  }

  public void setStatsEnable(Boolean statsEnable) {
    this.statsEnable = statsEnable;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatsEnable statsEnable = (StatsEnable) o;
    return Objects.equals(this.statsEnable, statsEnable.statsEnable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statsEnable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StatsEnable {\n");
    
    sb.append("    statsEnable: ").append(toIndentedString(statsEnable)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

