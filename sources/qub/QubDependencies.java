package qub;

public interface QubDependencies
{
    static void main(String[] args)
    {
        PreCondition.assertNotNull(args, "args");

        Process.run(args, QubDependencies::getParameters, QubDependencies::run);
    }

    static QubDependenciesParameters getParameters(Process process)
    {
        PreCondition.assertNotNull(process, "process");

        QubDependenciesParameters result = null;

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName("qub-dependencies")
            .setApplicationDescription("Perform operations on the dependencies of a project source folder.");
        final CommandLineParameterProfiler profilerParameter = parameters.addProfiler(process, QubDependencies.class);
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterHelp helpParameter = parameters.addHelp();

        if (!helpParameter.showApplicationHelpLines(process).await())
        {
            profilerParameter.await();

            final CharacterWriteStream output = process.getOutputCharacterWriteStream();
            final VerboseCharacterWriteStream verbose = verboseParameter.getVerboseCharacterWriteStream().await();
            final Folder folder = process.getCurrentFolder().await();
            final EnvironmentVariables environmentVariables = process.getEnvironmentVariables();

            result = new QubDependenciesParameters(output, verbose, folder, environmentVariables);
        }

        return result;
    }

    static int run(QubDependenciesParameters parameters)
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
                            final QubFolder qubFolder = new QubFolder(folder.getFileSystem(), qubHomePath);
                            final int dependencyCount = dependencies.getCount();
                            output.writeLine("Found " + dependencyCount + " " + (dependencyCount == 1 ? "dependency" : "dependencies") + ":").await();
                            final IndentedCharacterWriteStream indentedOutput = new IndentedCharacterWriteStream(output);
                            for (final ProjectSignature dependency : dependencies)
                            {
                                QubDependencies.writeDependencyTree(indentedOutput, dependency, qubFolder);
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
            final File dependencyProjectJsonFile = qubFolder.getProjectJSONFile(
                dependency.getPublisher(),
                dependency.getProject(),
                dependency.getVersion()).await();
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
                            QubDependencies.writeDependencyTree(output, dependencyDependency, qubFolder);
                        }
                    }
                }
            }
        });
    }
}