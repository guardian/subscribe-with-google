let count = 0;

const incrementCount = () => {
    count++;
}

const getCount = () => count;

module.exports = {
    incrementCount: incrementCount,
    getCount: getCount
}