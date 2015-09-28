package ut.com.isroot.stash.plugin;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.branch.BranchCreationRequestedEvent;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookDetails;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.i18n.I18nService;
import com.atlassian.stash.i18n.KeyedMessage;
import com.atlassian.stash.repository.Branch;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsBuilder;
import com.atlassian.stash.user.EscalatedSecurityContext;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.CancelState;
import com.atlassian.stash.util.UncheckedOperation;
import com.isroot.stash.plugin.YaccBranchCreationListener;

/**
 * @author Hiroyuki Wada
 * 
 */
public class YaccBranchCreationListenerTest {
    @Mock private RepositoryHookService repositoryHookService;
    @Mock private RepositoryHook repositoryHook;
    @Mock private RepositoryHookDetails repositoryHookDetails;
    @Mock private Repository repository;
    @Mock private Branch branch;
    @Mock private CancelState cancelState;
    @Mock private SecurityService securityService;
    @Mock private EscalatedSecurityContext escalatedSecurityContextForRepositoryHook;
    @Mock private EscalatedSecurityContext escalatedSecurityContextForSettings;
    @Mock private PluginSettingsFactory pluginSettingsFactory;
    @Mock private PluginSettings pluginSettings;
    @Mock private SettingsBuilder settingsBuilder;
    @Mock private Settings settings;
    @Mock private I18nService i18nService;
    @Mock private BranchCreationRequestedEvent event;
    @Mock private KeyedMessage message;
    private Map<String, Object> settingsMap = new HashMap<String, Object>();

    private YaccBranchCreationListener yaccBranchCreationListener;


    @Before
    public void setup() throws Throwable {
        MockitoAnnotations.initMocks(this);

        yaccBranchCreationListener = new YaccBranchCreationListener(
                pluginSettingsFactory, securityService, repositoryHookService, i18nService);

        //mock hook retrieval
        when(securityService.withPermission(Permission.REPO_ADMIN, "Get plugin configuration"))
                .thenReturn(escalatedSecurityContextForRepositoryHook);
        when(escalatedSecurityContextForRepositoryHook.call(any(UncheckedOperation.class))).thenReturn(repositoryHook);

        when(securityService.withPermission(Permission.REPO_ADMIN, "Get hook configuration"))
                .thenReturn(escalatedSecurityContextForSettings);
        when(escalatedSecurityContextForSettings.call(any(UncheckedOperation.class))).thenReturn(settings);
    }

    @Test
    public void testOnBranchCreation_errorIfBranchNameDoesNotMatchRegex_repositoryHookConfigured(){
        when(repositoryHook.isConfigured()).thenReturn(true);
        when(repositoryHook.isEnabled()).thenReturn(true);
        when(repositoryHook.getDetails()).thenReturn(repositoryHookDetails);
        when(repositoryHookDetails.getKey()).thenReturn(anyString());
        when(repositoryHookService.getSettings(repository, anyString())).thenReturn(settings);

        when(event.getBranch()).thenReturn(branch);
        when(branch.getId()).thenReturn("refs/heads/bar");
        when(settings.getString("branchNameRegex")).thenReturn("foo");
        when(i18nService.getKeyedText(anyString(), anyString())).thenReturn(message);

        yaccBranchCreationListener.onBranchCreation(event);

        verify(event, times(1)).cancel(message);
    }

    @Test
    public void testOnBranchCreation_noErrorIfBranchNameDoesNotMatchRegex_repositoryHookConfigured(){
        when(repositoryHook.isConfigured()).thenReturn(true);
        when(repositoryHook.isEnabled()).thenReturn(true);
        when(repositoryHook.getDetails()).thenReturn(repositoryHookDetails);
        when(repositoryHookDetails.getKey()).thenReturn(anyString());
        when(repositoryHookService.getSettings(repository, anyString())).thenReturn(settings);

        when(event.getBranch()).thenReturn(branch);
        when(branch.getId()).thenReturn("refs/heads/foo");
        when(settings.getString("branchNameRegex")).thenReturn("foo");
        when(i18nService.getKeyedText(anyString(), anyString())).thenReturn(message);

        yaccBranchCreationListener.onBranchCreation(event);

        verify(event, never()).cancel(message);
    }

    @Test
    public void testOnBranchCreation_errorIfBranchNameDoesNotMatchRegex_globalConfigured(){
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(anyString())).thenReturn(settingsMap);
        when(repositoryHookService.createSettingsBuilder()).thenReturn(settingsBuilder);
        when(settingsBuilder.addAll(anyMap())).thenReturn(settingsBuilder);
        when(settingsBuilder.build()).thenReturn(settings);

        when(event.getBranch()).thenReturn(branch);
        when(branch.getId()).thenReturn("refs/heads/bar");
        when(settings.getString("branchNameRegex")).thenReturn("foo");
        when(i18nService.getKeyedText(anyString(), anyString())).thenReturn(message);

        yaccBranchCreationListener.onBranchCreation(event);

        verify(event, times(1)).cancel(message);
    }

    @Test
    public void testOnBranchCreation_noErrorIfBranchNameDoesNotMatchRegex_globalConfigured(){
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(anyString())).thenReturn(settingsMap);
        when(repositoryHookService.createSettingsBuilder()).thenReturn(settingsBuilder);
        when(settingsBuilder.addAll(anyMap())).thenReturn(settingsBuilder);
        when(settingsBuilder.build()).thenReturn(settings);

        when(event.getBranch()).thenReturn(branch);
        when(branch.getId()).thenReturn("refs/heads/foo");
        when(settings.getString("branchNameRegex")).thenReturn("foo");
        when(i18nService.getKeyedText(anyString(), anyString())).thenReturn(message);

        yaccBranchCreationListener.onBranchCreation(event);

        verify(event, never()).cancel(message);
    }
}
