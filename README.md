# Kafka Connect Policy Extension

This is a lightweight extension for Kafka Connect Workers to perform additional checks on the Kafka Connect REST API.

When Kafka Connect is managed as a cluster for multiple users/teams, then we may want to enforce certain policies for
connectors that are configured in the Kafka Connect cluster.

Policies can be defined as json schemas or by using other PolicyCheck implementations.

Supported Checks:

* ConnectorNameCheck
* JsonSchemaCheck

## ConnectorNameCheck

Validates the connector name against a regular expression.

To enforce hat a connector name starts with "dev.", "tst." or "prd.", you can use:
```json
{
    "name": "env-prefix-in-name",
    "className": "ConnectorNameCheck",
    "config": {"regex": "^((dev)|(tst)|(prd))\\..+$"}
}
```

## JsonSchemaCheck

Checks the connector JSON configuration against a json schema.
The json schema must be placed in the same directory where the extension configuration file is placed.
The schema file is then referenced by name.

```json
{
    "name": "limit-connector-tasks",
    "className": "JsonSchemaCheck",
    "config": {
        "file": "limit-connector-tasks.json"
    }
}
```


### Restricting the number of tasks

The connector config `tasks.max` limit the number of tasks that can be created for a connector.
If you operate Kafka Connect and want to set an upper bound for this setting, then using a JsonSchema like this would do
the trick:

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "properties": {
        "tasks.max": {
            "type": "number",
            "exclusiveMaximum": 4,
            "description": "max number of tasks for a connector"
        }
    },
    "required": [
        "tasks.max"
    ]
}
```

### Require custom meta data for connectors

Kafka connector configurations can have additional configuration properties without being considered invalid by the
connect worker.
We can use this to add extra fields for management purposes.
E.g. for alerting, you may want to know who to contact in case a connector is in a failed state.

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "properties": {
        "contact.email": {
            "type": "string",
            "minLength": 5,
            "description": "team email address to be contacted when the connector is affected by maintenance work or failures"
        }
    },
    "required": [
        "contact.email"
    ]
}
```

## Installation

1. Place the JAR file in the plugins folder on each connect worker
2. Create policy config files, see the example in [examples/rest-policy-config.json](examples/rest-policy-config.json)
3. Reference the policy config file and the `RestPolicyExtension` extension class in the Connect worker properties:

       rest.extension.classes: "io.spoud.kafka.connect.extensions.RestPolicyExtension"
       rest.policy.conf: "/etc/kafka-connect/policy/rest-policy-config.json"

## Changing policies at runtime without worker restart

A change to the property `rest.policy.conf` requires a restart of the worker to take effect.
However, a change in the referenced json file can be reloaded at runtime by calling the reload-endpoint
with `PUT /policies/reload`.
