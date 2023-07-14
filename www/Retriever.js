var exec = require('cordova/exec');

const retriever = {
    getPreview(success, error, theUri, itemId, maxSideSize, maxPreviewSideSize, jpegQuality) {
        exec(success, error, "Retriever", "getPreview", [theUri, itemId, maxSideSize, maxPreviewSideSize, jpegQuality]);
    },
    getVideoPreview(success, error, theUri, itemId = 0) {
        exec(success, error, "Retriever", "getVideoPreview", [theUri, itemId]);
    },
    getAudioInfo(success, error, theUri, itemId = 0) {
        exec(success, error, "Retriever", "getAudioInfo", [theUri, itemId]);
    },
    mediaUpload(success, error, holyPack, extraPack) {
        let ep = JSON.stringify(extraPack);
        exec(success, error, "Retriever", "uploadMedia", 
        [ holyPack.itemId, holyPack.theUri, holyPack.mediaType, holyPack.queryUrl, holyPack.token,
           holyPack.chatId, holyPack.fromUser, ep ]
        );
    },
    getFiles(success, error) { console.log('------> getFiles in retriever.js works');// original
        exec(success, error, "Retriever", "getFilesFromPicker");
    }
}

module.exports = retriever;