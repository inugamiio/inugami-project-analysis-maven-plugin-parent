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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@Table(name = "ISSUE_ENTITY")
@SqlResultSetMapping(name = IssueEntity.NATIVE_QUERY_GET_BY_AUTHOR,
                     classes = {
                             @ConstructorResult(
                                     targetClass = IssueEntity.class,
                                     columns = {
                                             @ColumnResult(name = "uid", type = Long.class),
                                             @ColumnResult(name = "title", type = String.class),
                                             @ColumnResult(name = "description", type = String.class),
                                             @ColumnResult(name = "lifecycle", type = String.class),
                                             @ColumnResult(name = "previous_known_state", type = String.class),
                                             @ColumnResult(name = "create_by", type = String.class),
                                             @ColumnResult(name = "created_date", type = Calendar.class)
                                     })
                     }
)
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "getByAuthor",
                query = "SELECT uid,title,description,lifecycle,previous_known_state,create_by,created_date " +
                        "FROM ISSUE_ENTITY as issue WHERE create_by=:name",
                resultClass = IssueEntity.class,
                resultSetMapping = IssueEntity.NATIVE_QUERY_GET_BY_AUTHOR
        )
})
@Entity
public class IssueEntity {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public final static String NATIVE_QUERY_GET_BY_AUTHOR            = "getByAuthor";
    public final static String NATIVE_QUERY_GET_BY_AUTHOR_PARAM_NAME = "name";

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long uid;

    @NotNull
    private String title;

    private String description;

    private String lifecycle;
    private String previousKnownState;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Calendar createdDate;

    @OneToMany(targetEntity = ChangeLogEntity.class, cascade = CascadeType.ALL)
    private List<ChangeLogEntity> changeLogs;

    @ManyToOne(fetch = FetchType.EAGER)
    private ChangeLogEntity status;

    @ManyToOne(fetch = FetchType.LAZY)
    private IssueEntity parent;


    public void addChangeLogs(final List<ChangeLogEntity> changeLogsEntities) {
        if (changeLogs == null) {
            changeLogs = new ArrayList<>();
        }
        if (changeLogsEntities != null) {
            changeLogs.addAll(changeLogsEntities);
        }
    }

}
