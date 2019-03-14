/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.routing.router.masterslave;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.api.hint.HintManager;
import org.apache.shardingsphere.core.constant.SQLType;
import org.apache.shardingsphere.core.routing.SQLRouteResult;
import org.apache.shardingsphere.core.routing.type.TableUnit;
import org.apache.shardingsphere.core.rule.MasterSlaveRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Sharding with master-slave router interface.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class ShardingMasterSlaveRouter {
    
    private final Collection<MasterSlaveRule> masterSlaveRules;
    
    /**
     * Route Master slave after sharding.
     * 
     * @param sqlRouteResult SQL route result
     * @return route result
     */
    public SQLRouteResult route(final SQLRouteResult sqlRouteResult) {
        for (MasterSlaveRule each : masterSlaveRules) {
            route(each, sqlRouteResult);
        }
        return sqlRouteResult;
    }
    
    private void route(final MasterSlaveRule masterSlaveRule, final SQLRouteResult sqlRouteResult) {
        Collection<TableUnit> toBeRemoved = new LinkedList<>();
        Collection<TableUnit> toBeAdded = new LinkedList<>();
        for (TableUnit each : sqlRouteResult.getRoutingResult().getTableUnits().getTableUnits()) {
            if (!masterSlaveRule.getName().equalsIgnoreCase(each.getDataSourceName())) {
                continue;
            }
            toBeRemoved.add(each);
            String actualDataSourceName;
            if (isMasterRoute(sqlRouteResult.getSqlStatement().getType())) {
                MasterVisitedManager.setMasterVisited();
                actualDataSourceName = masterSlaveRule.getMasterDataSourceName();
            } else {
                actualDataSourceName = masterSlaveRule.getLoadBalanceAlgorithm().getDataSource(
                        masterSlaveRule.getName(), masterSlaveRule.getMasterDataSourceName(), new ArrayList<>(masterSlaveRule.getSlaveDataSourceNames()));
            }
            toBeAdded.add(createNewTableUnit(actualDataSourceName, each));
        }
        sqlRouteResult.getRoutingResult().getTableUnits().getTableUnits().removeAll(toBeRemoved);
        sqlRouteResult.getRoutingResult().getTableUnits().getTableUnits().addAll(toBeAdded);
    }
    
    private boolean isMasterRoute(final SQLType sqlType) {
        return SQLType.DQL != sqlType || MasterVisitedManager.isMasterVisited() || HintManager.isMasterRouteOnly();
    }
    
    private TableUnit createNewTableUnit(final String actualDataSourceName, final TableUnit originalTableUnit) {
        TableUnit result = new TableUnit(actualDataSourceName, originalTableUnit.getDataSourceName());
        result.getRoutingTables().addAll(originalTableUnit.getRoutingTables());
        return result;
    }
}
