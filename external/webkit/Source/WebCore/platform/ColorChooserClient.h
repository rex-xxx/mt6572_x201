#ifndef ColorChooserClient_h
#define ColorChooserClient_h

#include <wtf/OwnPtr.h>
#include <wtf/PassOwnPtr.h>

namespace WebCore {

class Color;

class ColorChooserClient {
public:
    virtual ~ColorChooserClient() { }

    virtual void didChooseColor(const Color&) = 0;

    virtual Color* getColor() = 0;
};

} // namespace WebCore

#endif // ColorChooserClient_h
