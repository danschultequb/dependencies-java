package qub;

public interface QubDependenciesUpdate
{
    String actionName = "update";
    String actionDescription = "Update the dependencies of a project.";

    static QubDependenciesUpdateParameters getParameters(Process process)
    {
        PreCondition.assertNotNull(process, "process");

        QubDependenciesUpdateParameters result = null;

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName(QubDependencies.getActionFullName(QubDependenciesUpdate.actionName))
            .setApplicationDescription(QubDependenciesUpdate.actionDescription);
        final CommandLineParameterProfiler profilerParameter = parameters.addProfiler(process, QubDependenciesUpdate.class);
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterHelp helpParameter = parameters.addHelp();

        if (!helpParameter.showApplicationHelpLines(process).await())
        {
            profilerParameter.await();

            final CharacterWriteStream output = process.getOutputCharacterWriteStream();
            final VerboseCharacterWriteStream verbose = verboseParameter.getVerboseCharacterWriteStream().await();
            final Folder folder = process.getCurrentFolder().await();
            final EnvironmentVariables environmentVariables = process.getEnvironmentVariables();

            result = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);
        }

        return result;
    }

    static int run(QubDependenciesUpdateParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        int exitCode = 0;

        final CharacterWriteStream output = parameters.getOutput();
        final VerboseCharacterWriteStream verbose = parameters.getVerbose();
        final Folder folder = parameters.getFolder();
        final EnvironmentVariables environmentVariables = parameters.getEnvironmentVariables();

        output.writeLine("Updating dependencies for " + folder + "...").await();

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
                            indentedOutput.indent(() ->
                            {
                                boolean dependenciesChanged = false;
                                final List<ProjectSignature> newDependencies = List.create();
                                for (final ProjectSignature dependency : dependencies)
                                {
                                    indentedOutput.write(dependency.toString()).await();

                                    final QubProjectFolder projectFolder = qubFolder.getProjectFolder(dependency.getPublisher(), dependency.getProject()).await();
                                    QubProjectVersionFolder latestVersionFolder = projectFolder.getProjectVersionFolder(dependency.getVersion()).await();
                                    Integer latestVersionFolderNumber = Integers.parse(latestVersionFolder.getVersion()).await();
                                    final Iterable<QubProjectVersionFolder> versionFolders = projectFolder.getProjectVersionFolders().await();
                                    if (!versionFolders.any())
                                    {
                                        newDependencies.add(dependency);
                                        indentedOutput.writeLine(" - Not Found").await();
                                    }
                                    else
                                    {
                                        for (QubProjectVersionFolder versionFolder : versionFolders)
                                        {
                                            final Integer versionFolderNumber = Integers.parse(versionFolder.getVersion()).await();
                                            if (Comparer.lessThan(latestVersionFolderNumber, versionFolderNumber))
                                            {
                                                latestVersionFolder = versionFolder;
                                                latestVersionFolderNumber = versionFolderNumber;
                                            }
                                        }

                                        if (Comparer.equal(dependency.getVersion(), latestVersionFolder.getVersion()))
                                        {
                                            newDependencies.add(dependency);
                                            indentedOutput.writeLine(" - No Updates").await();
                                        }
                                        else
                                        {
                                            final ProjectSignature newDependency = new ProjectSignature(dependency.getPublisher(), dependency.getProject(), latestVersionFolder.getVersion());
                                            newDependencies.add(newDependency);
                                            dependenciesChanged = true;
                                            indentedOutput.writeLine(" - Updated to " + newDependency).await();
                                        }
                                    }
                                }

                                if (dependenciesChanged)
                                {
                                    projectJSONJava.setDependencies(newDependencies);
                                    projectJsonFile.setContentsAsString(projectJSON.toString(JSONFormat.pretty)).await();
                                }
                            });
                        }
                    }
                }
            }
        }

        return exitCode;
    }
}
