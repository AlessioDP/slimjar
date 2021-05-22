package io.github.slimjar.app.builder;

import io.github.slimjar.app.Application;
import io.github.slimjar.injector.DependencyInjector;
import io.github.slimjar.injector.loader.InjectableClassLoader;
import io.github.slimjar.injector.loader.IsolatedInjectableClassLoader;
import io.github.slimjar.resolver.data.DependencyData;
import io.github.slimjar.resolver.reader.DependencyDataProvider;
import io.github.slimjar.resolver.reader.DependencyDataProviderFactory;
import io.github.slimjar.util.Modules;
import io.github.slimjar.util.Parameters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public final class IsolatedApplicationBuilder extends ApplicationBuilder {
    private final IsolationConfiguration isolationConfiguration;
    private final Object[] arguments;

    public IsolatedApplicationBuilder(final String applicationName, final IsolationConfiguration isolationConfiguration, final Object[] arguments) {
        super(applicationName);
        this.isolationConfiguration = isolationConfiguration;
        this.arguments = arguments;
    }

    @Override
    public Application build() throws IOException, ReflectiveOperationException, URISyntaxException, NoSuchAlgorithmException {
        final DependencyDataProviderFactory dataProviderFactory = getDependencyProvider().createDependencyDataProviderFactory();
        final DependencyInjector injector = createInjector(dataProviderFactory);
        final URL[] moduleUrls = Modules.extract(isolationConfiguration.getModuleExtractor(), isolationConfiguration.getModules());

        final InjectableClassLoader classLoader = new IsolatedInjectableClassLoader(moduleUrls, isolationConfiguration.getParentClassloader(), Collections.singleton(Application.class));

        for (final URL module : moduleUrls) {
            final DependencyDataProvider moduleDataProvider = dataProviderFactory.forModule(module);
            final DependencyData dependencyData = moduleDataProvider.get();
            injector.inject(classLoader, dependencyData.getDependencies());
        }

        final Class<Application> applicationClass = (Class<Application>) Class.forName(isolationConfiguration.getApplicationClass(), true, classLoader);

        // TODO:: Fix constructor resolution
        return applicationClass.getConstructor(Parameters.typesFrom(arguments)).newInstance(arguments);
    }

}