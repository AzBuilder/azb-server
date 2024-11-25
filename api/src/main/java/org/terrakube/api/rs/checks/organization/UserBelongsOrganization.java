package org.terrakube.api.rs.checks.organization;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.OperationCheck;
import lombok.extern.slf4j.Slf4j;
import org.terrakube.api.plugin.security.groups.GroupService;
import org.terrakube.api.plugin.security.user.AuthenticatedUser;
import org.terrakube.api.rs.Organization;
import org.terrakube.api.rs.team.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.terrakube.api.rs.workspace.Workspace;
import org.terrakube.api.rs.workspace.access.Access;

import java.util.List;
import java.util.Optional;

@Slf4j
@SecurityCheck(UserBelongsOrganization.RULE)
public class UserBelongsOrganization extends OperationCheck<Organization> {

    public static final String RULE = "user belongs organization";

    @Autowired
    AuthenticatedUser authenticatedUser;

    @Autowired
    GroupService groupService;

    @Override
    public boolean ok(Organization organization, RequestScope requestScope, Optional<ChangeSpec> optional) {
        if(authenticatedUser.isSuperUser(requestScope.getUser())){
            return true;
        }else{
            if (isMemberOrganization(requestScope.getUser(), organization)) {
                return true;
            } else {
                return isMemberOrganizationWithLimitedWorkspaceAccess(requestScope.getUser(), organization);
            }
        }
    }

    /**
     * Review if the authenticated user belongs to the organization by searching the membership in each organization team
     * @param user
     * @param organization
     * @return
     */
    private boolean isMemberOrganization(User user, Organization organization){
        boolean isServiceAccount=authenticatedUser.isServiceAccount(user);

        List<Team> teamList = organization.getTeam();
        for (Team team : teamList) {
            if (isServiceAccount) {
                boolean isServiceMember = groupService.isServiceMember(user, team.getName());
                log.debug("isServiceMember {} {}", team.getName(), isServiceMember);
                if (isServiceMember) {
                    return true;
                }
            } else {
                boolean isMember = groupService.isMember(user, team.getName());
                log.debug("isMember {} {}", team.getName(), isMember);
                if (isMember)
                    return true;
            }
        }
        return false;
    }

    /**
     * Review if the authenticated user belongs to the organization by searching the membership in all workspace inside the organization
     * @param user
     * @param organization
     * @return
     */
    private boolean isMemberOrganizationWithLimitedWorkspaceAccess(User user, Organization organization){
        for (Workspace workspace: organization.getWorkspace()){
            List<Access> teamList = workspace.getAccess();
            if (!teamList.isEmpty())
                for (Access team : teamList) {
                    boolean isMember = groupService.isMember(user, team.getName());
                    log.debug("isMember {} {}", team.getName(), isMember);
                    if (isMember)
                        return true;
                }
        }
        return false;
    }


}
