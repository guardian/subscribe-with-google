function generatePolicy(resource, principalId) {
    return {
        principalId: principalId,
        policyDocument: {
            Version: '2012-10-17',
            Statement: [{
                Action: 'execute-api:Invoke',
                Effect: 'Allow',
                Resource: resource
            }]
        }
    };
}

exports.handler = (event, context, callback) => {
    const secret = event.queryStringParameters.secret;
    
    if (secret !== process.env.SECRET_KEY) {
        callback('Invalid secret');
    } else {
        callback(null, generatePolicy(event.methodArn, secret));
    }
};
