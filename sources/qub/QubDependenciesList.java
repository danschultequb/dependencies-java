package qub;

public interface QubDependenciesList
{
    String actionName = "list";
    String actionDescription = "List the dependencies of a project.";

    static QubDependenciesListParameters getParameters(QubProcess process)
    {
        PreCondition.assertNotNull(process, "process");

        QubDependenciesListParameters result = null;

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName(QubDependencies.getActionFullName(QubDependenciesList.actionName))
            .setApplicationDescription(QubDependenciesList.actionDescription);
        final CommandLineParameterProfiler profilerParameter = parameters.addProfiler(process, QubDependenciesList.class);
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterHelp helpParameter = parameters.addHelp();

        if (!helpParameter.showApplicationHelpLines(process).await())
        {
            profilerParameter.await();

            final CharacterWriteStream output = process.getOutputCharacterWriteStream();
            final VerboseCharacterWriteStream verbose = verboseParameter.getVerboseCharacterWriteStream().await();
            final Folder folder = process.getCurrentFolder().await();
            final EnvironmentVariables environmentVariables = process.getEnvironmentVariables();

            result = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);
        }

        return result;
    }

    static int run(QubDependenciesListParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        int exitCode = 0;

        final CharacterWriteStream output = parameters.getOutput();
        final VerboseCharacterWriteStream verbose = parameters.getVerbose();
        final Folder folder = parameters.getFolder();
        final EnvironmentVariables environmentVariables = parameters.getEnvironmentVariables();

        output.writeLine("Getting dependencies for " + folder + "...").await();

        final File projectJsonFile = folder.getFile("project.json").await();
        final ProjectJSON projectJSON = ProjectJSON.parse(projectJsonFile)
            .catchError(FileNotFoundException.class)
            .await();
        if (projectJSON == null)
        {
            output.writeLine("No project.json file found at " + projectJsonFile + ".").await();
            exitCode = 1;
        }
        else
        {
            final ProjectJSONJava projectJSONJava = projectJSON.getJava();
            if (projectJSONJava == null)
            {
                output.writeLine("No \"java\" property found in " + projectJsonFile + ".").await();
                exitCode = 1;
            }
            else
            {
                final Iterable<ProjectSignature> dependencies = projectJSONJava.getDependencies();
                if (Iterable.isNullOrEmpty(dependencies))
                {
                    output.writeLine("No dependencies found in " + projectJsonFile + ".").await();
                }
                else
                {
                    final String qubHome = environmentVariables.get("QUB_HOME")
                        .catchError(NotFoundException.class)
                        .await();
                    if (Strings.isNullOrEmpty(qubHome))
                    {
                        output.writeLine("Can't discover transitive dependencies if a QUB_HOME environment variable is not specified.").await();
                        exitCode = 1;
                    }
                    else
                    {
                        final Path qubHomePath = Path.parse(qubHome);
                        if (!qubHomePath.isRooted())
                        {
                            output.writeLine("Can't discover transitive dependencies if the QUB_HOME environment variable is not rooted.").await();
                            exitCode = 1;
                        }
                        else
                        {
                            final QubFolder qubFolder = QubFolder.get(folder.getFileSystem().getFolder(qubHomePath).await());
                            final int dependencyCount = dependencies.getCount();
                            output.writeLine("Found " + dependencyCount + " " + (dependencyCount == 1 ? "dependency" : "dependencies") + ":").await();
                            final IndentedCharacterWriteStream indentedOutput = new IndentedCharacterWriteStream(output);
                            for (final ProjectSignature dependency : dependencies)
                            {
                                QubDependenciesList.writeDependencyTree(indentedOutput, dependency, qubFolder);
                            }
                        }
                    }
                }
            }
        }

        return exitCode;
    }

    static void writeDependencyTree(IndentedCharacterWriteStream output, ProjectSignature dependency, QubFolder qubFolder)
    {
        output.indent(() ->
        {
            File dependencyProjectJsonFile = qubFolder.getProjectJSONFile2(
                dependency.getPublisher(),
                dependency.getProject(),
                dependency.getVersion()).await();
            if (!dependencyProjectJsonFile.exists().await())
            {
                dependencyProjectJsonFile = qubFolder.getProjectJSONFile(
                    dependency.getPublisher(),
                    dependency.getProject(),
                    dependency.getVersion()).await();
            }
            final ProjectJSON dependencyProjectJson = ProjectJSON.parse(dependencyProjectJsonFile)
                .catchError(FileNotFoundException.class)
                .await();

            output.write(dependency.toString()).await();
            if (dependencyProjectJson == null)
            {
                output.writeLine(" - Not Found").await();
            }
            else
            {
                final ProjectJSONJava dependencyProjectJsonJava = dependencyProjectJson.getJava();
                if (dependencyProjectJsonJava == null)
                {
                    output.writeLine(" - No Java Property").await();
                }
                else
                {
                    output.writeLine().await();
                    final Iterable<ProjectSignature> dependencyDependencies = dependencyProjectJsonJava.getDependencies();
                    if (!Iterable.isNullOrEmpty(dependencyDependencies))
                    {
                        for (final ProjectSignature dependencyDependency : dependencyDependencies)
                        {
                            QubDependenciesList.writeDependencyTree(output, dependencyDependency, qubFolder);
                        }
                    }
                }
            }
        });
    }
}