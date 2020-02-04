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
        final CommandLineParameterBoolean intellijParameter = parameters.addBoolean("intellij", true)
            .setDescription("Whether or not to update IntelliJ project files.");
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
            final boolean intellij = intellijParameter.getValue().await();

            result = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                .setIntellij(intellij);
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
        final boolean intellij = parameters.getIntellij();

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
                            final QubFolder qubFolder = QubFolder.create(folder.getFileSystem().getFolder(qubHomePath).await());
                            final int dependencyCount = dependencies.getCount();
                            output.writeLine("Found " + dependencyCount + " " + (dependencyCount == 1 ? "dependency" : "dependencies") + ":").await();
                            final IndentedCharacterWriteStream indentedOutput = new IndentedCharacterWriteStream(output);
                            final List<ProjectSignature> newDependencies = List.create();
                            indentedOutput.indent(() ->
                            {
                                boolean dependenciesChanged = false;
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
                                            indentedOutput.writeLine(" - No updates").await();
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

                            if (intellij)
                            {
                                final Iterable<File> intellijProjectFiles = folder.getFiles().await()
                                    .where((File file) -> Comparer.equal(file.getFileExtension(), ".iml"));
                                if (intellijProjectFiles.any())
                                {
                                    indentedOutput.writeLine("Updating IntelliJ project files...").await();
                                    indentedOutput.indent(() ->
                                    {
                                        if (!intellijProjectFiles.any())
                                        {
                                            indentedOutput.writeLine("No IntelliJ project files found.").await();
                                        }
                                        else
                                        {
                                            for (final File intellijProjectFile : intellijProjectFiles)
                                            {
                                                final IntellijModule intellijModule = IntellijModule.parse(intellijProjectFile)
                                                    .catchError(PreConditionFailure.class, () -> indentedOutput.writeLine("Invalid Intellij Module file: " + intellijProjectFile).await())
                                                    .await();
                                                if (intellijModule != null)
                                                {
                                                    final List<ProjectSignature> dependenciesToAddToModule = projectJSONJava.getTransitiveDependencies(qubFolder).toList();
                                                    final Iterable<IntellijModuleLibrary> currentModuleLibraries = intellijModule.getModuleLibraries().toList();

                                                    intellijModule.clearModuleLibraries();

                                                    for (final IntellijModuleLibrary moduleLibrary : currentModuleLibraries)
                                                    {
                                                        final String classesUrl = moduleLibrary.getClassesUrls().first();
                                                        if (Strings.isNullOrEmpty(classesUrl) || !classesUrl.startsWith("jar://"))
                                                        {
                                                            intellijModule.addModuleLibrary(moduleLibrary);
                                                        }
                                                        else
                                                        {
                                                            final int startIndex = "jar://".length();
                                                            int endIndex = classesUrl.length();
                                                            if (classesUrl.endsWith("!/"))
                                                            {
                                                                endIndex -= "!/".length();
                                                            }
                                                            final Path compiledSourcesFilePath = Path.parse(classesUrl.substring(startIndex, endIndex));
                                                            if (!qubFolder.isAncestorOf(compiledSourcesFilePath).await())
                                                            {
                                                                indentedOutput.writeLine(compiledSourcesFilePath + " - No updates").await();
                                                                intellijModule.addModuleLibrary(moduleLibrary);
                                                            }
                                                            else
                                                            {
                                                                final Path compiledSourcesRelativeFilePath = compiledSourcesFilePath.relativeTo(qubFolder);
                                                                final Indexable<String> segments = compiledSourcesRelativeFilePath.getSegments();
                                                                final String publisher = segments.get(0);
                                                                final String project = segments.get(1);
                                                                final String version = segments.get(2);
                                                                final ProjectSignature currentQubDependency = new ProjectSignature(publisher, project, version);

                                                                final ProjectSignature newQubDependency = dependenciesToAddToModule.removeFirst(currentQubDependency::equalsIgnoreVersion);
                                                                if (newQubDependency == null)
                                                                {
                                                                    indentedOutput.writeLine(currentQubDependency + " - Removed").await();
                                                                }
                                                                else
                                                                {
                                                                    if (newQubDependency.equals(currentQubDependency))
                                                                    {
                                                                        indentedOutput.writeLine(currentQubDependency + " - No updates").await();
                                                                    }
                                                                    else
                                                                    {
                                                                        indentedOutput.writeLine(currentQubDependency + " - Updated to " + newQubDependency).await();
                                                                    }

                                                                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder(
                                                                        newQubDependency.getPublisher(),
                                                                        newQubDependency.getProject(),
                                                                        newQubDependency.getVersion())
                                                                        .await();
                                                                    intellijModule.addModuleLibrary(IntellijModuleLibrary.create()
                                                                        .addClassesUrl("jar://" + projectVersionFolder.getCompiledSourcesFile().await().toString() + "!/")
                                                                        .addSourcesUrl("jar://" + projectVersionFolder.getSourcesFile().await().toString() + "!/"));
                                                                }
                                                            }
                                                        }
                                                    }

                                                    for (final ProjectSignature dependencyToAddToModule : dependenciesToAddToModule)
                                                    {
                                                        indentedOutput.writeLine(dependencyToAddToModule + " - Added").await();

                                                        final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder(
                                                            dependencyToAddToModule.getPublisher(),
                                                            dependencyToAddToModule.getProject(),
                                                            dependencyToAddToModule.getVersion())
                                                            .await();
                                                        intellijModule.addModuleLibrary(IntellijModuleLibrary.create()
                                                            .addClassesUrl("jar://" + projectVersionFolder.getCompiledSourcesFile().await().toString() + "!/")
                                                            .addSourcesUrl("jar://" + projectVersionFolder.getSourcesFile().await().toString() + "!/"));
                                                    }

                                                    intellijProjectFile.setContentsAsString(intellijModule.toString(XMLFormat.pretty)).await();
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }
        }

        return exitCode;
    }
}
