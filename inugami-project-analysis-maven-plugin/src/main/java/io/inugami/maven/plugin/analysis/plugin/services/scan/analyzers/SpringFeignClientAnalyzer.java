/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;


import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

@Slf4j
public class SpringFeignClientAnalyzer extends SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final  String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.feign";
    public static final  String FEATURE      = FEATURE_NAME + ".enable";
    private static final String CONSUME      = "CONSUME";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && clazz.getAnnotation(FeignClient.class) != null;
    }

    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        return super.analyze(clazz, context);
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    protected String getApiName(final Class<?> clazz) {
        return ReflectionService
                .ifHasAnnotation(clazz, FeignClient.class, FeignClient::name, () -> clazz.getSimpleName());
    }

    @Override
    protected String getBaseContext(final Class<?> clazz) {
        return ReflectionService.ifHasAnnotation(clazz, FeignClient.class, FeignClient::path);
    }

    @Override
    protected String getRelationshipType() {
        return CONSUME;
    }
}
