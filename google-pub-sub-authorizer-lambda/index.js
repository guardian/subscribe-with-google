function generatePolicy(effect, resource) {
    return {
        principalId: 'user',
        policyDocument: {
            Version: '2012-10-17',
            Statement: [{
                Action: 'execute-api:Invoke',
                Effect: effect,
                Resource: resource
            }]
        }
    };
}

exports.handler = (event, context, cb) => {
    const secret = event.queryStringParameters.secret;
    if (secret == process.env.SECRET_KEY) {
        cb(null, generatePolicy('Allow', event.methodArn));
    } else {
        cb(null, generatePolicy('Deny', event.methodArn));
    }
}
