package qub;

public interface QubDependencies
{
    String applicationName = "qub-dependencies";
    String applicationDescription = "Perform operations on the dependencies of a project source folder.";

    static void main(String[] args)
    {
        PreCondition.assertNotNull(args, "args");

        QubProcess.run(args, QubDependencies::run);
    }

    static void run(QubProcess process)
    {
        PreCondition.assertNotNull(process, "process");

        final CommandLineActions<QubProcess> actions = process.<QubProcess>createCommandLineActions()
            .setApplicationName(QubDependencies.applicationName)
            .setApplicationDescription(QubDependencies.applicationDescription);

        actions.addAction(QubDependenciesList.actionName, QubDependenciesList::getParameters, QubDependenciesList::run)
            .setDescription(QubDependenciesList.actionDescription);
        actions.addAction(QubDependenciesUpdate.actionName, QubDependenciesUpdate::getParameters, QubDependenciesUpdate::run)
            .setDescription(QubDependenciesUpdate.actionDescription);

        actions.run(process);
    }

    static String getActionFullName(String actionName)
    {
        PreCondition.assertNotNullAndNotEmpty(actionName, "actionName");

        return QubDependencies.applicationName + " " + actionName;
    }
}
