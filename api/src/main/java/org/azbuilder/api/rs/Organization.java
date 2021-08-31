package org.azbuilder.api.rs;

import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;
import org.azbuilder.api.rs.job.Job;
import org.azbuilder.api.rs.module.Module;
import org.azbuilder.api.rs.provider.Provider;
import org.azbuilder.api.rs.workspace.Workspace;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Include
@Getter
@Setter
@Entity
public class Organization {

    @Id
    @Type(type="uuid-char")
    @GeneratedValue
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "owner")
    private String owner;

    @OneToMany(mappedBy = "organization")
    private List<Workspace> workspace;

    @OneToMany(mappedBy = "organization")
    private List<Module> module;

    @OneToMany(mappedBy = "organization")
    private List<Provider> provider;

    @OneToMany(mappedBy = "organization")
    private List<Job> job;
}
