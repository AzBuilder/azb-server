package org.terrakube.api.rs.template;

import com.yahoo.elide.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.terrakube.api.plugin.security.audit.GenericAuditFields;
import org.terrakube.api.rs.Organization;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.UUID;

@ReadPermission(expression = "team view template")
@CreatePermission(expression = "team manage template")
@UpdatePermission(expression = "team manage template")
@DeletePermission(expression = "team manage template")
@Include
@Getter
@Setter
@Entity(name = "template")
public class Template extends GenericAuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "version")
    private String version;

    @Column(name = "tcl")
    private String tcl;

    @ManyToOne
    private Organization organization;
}
