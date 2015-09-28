package com.isroot.stash.plugin;

import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.branch.BranchCreationRequestedEvent;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.i18n.I18nService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.UncheckedOperation;
import com.isroot.stash.plugin.checks.BranchNameCheck;
import com.isroot.stash.plugin.errors.YaccError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Hiroyuki Wada
 */
public class YaccBranchCreationListener {

    private static final Logger log = LoggerFactory.getLogger(YaccBranchCreationListener.class);

    private final PluginSettingsFactory pluginSettingsFactory;
    private final SecurityService securityService;
    private final RepositoryHookService repositoryHookService;
    private final I18nService i18nService;

    public YaccBranchCreationListener(PluginSettingsFactory pluginSettingsFactory, SecurityService securityService,
            RepositoryHookService repositoryHookService, I18nService i18nService) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.securityService = securityService;
        this.repositoryHookService = repositoryHookService;
        this.i18nService = i18nService;
    }

    @EventListener
    public void onBranchCreation(BranchCreationRequestedEvent event) {
        final Repository repository = event.getRepository();

        final RepositoryHook hook = securityService.withPermission(Permission.REPO_ADMIN, "Get plugin configuration").call(
                new UncheckedOperation<RepositoryHook>() {
                    public RepositoryHook perform() {
                        return repositoryHookService.getByKey(repository, "com.isroot.stash.plugin.yacc:yaccHook");
                    }
                });

        Settings settings = null;

        if (hook.isEnabled() && hook.isConfigured()) {
            // Repository hook is configured and enabled.
            // Repository hook overrides default pre-receive hook configuration
            log.debug("PreReceiveRepositoryHook configured. Use repository configuration.");

            settings = securityService.withPermission(Permission.REPO_ADMIN, "Get hook configuration").call(
                    new UncheckedOperation<Settings>() {
                        public Settings perform() {
                            return repositoryHookService.getSettings(repository, hook.getDetails().getKey());
                        }
                    });
        } else {
            // Repository hook not configured
            log.debug("PreReceiveRepositoryHook not configured.  Use global configuration.");

            settings = YaccUtils.buildYaccConfig(pluginSettingsFactory, repositoryHookService);
        }

        List<YaccError> errors = new BranchNameCheck(settings, event.getBranch().getId()).check();

        if (!errors.isEmpty()) {
            event.cancel(i18nService.getKeyedText("invalidBranchName", errors.get(0).getMessage()));
        }
    }
}
