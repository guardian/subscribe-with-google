let count = 0;

const incrementCount = () => {
    count++;
}

const getCount = () => count;

const resetCount = () => {
    count = 0;
}
module.exports = {
    incrementCount: incrementCount,
    getCount: getCount,
    resetCount
}